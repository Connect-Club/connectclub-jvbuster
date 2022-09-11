package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.monitoring.i.JvbConferencesTasksService;
import com.connectclub.jvbuster.repository.data.JvbConferenceData;
import com.connectclub.jvbuster.repository.data.JvbEndpointData;
import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import com.connectclub.jvbuster.repository.i.JvbConferenceDataRepository;
import com.connectclub.jvbuster.repository.i.JvbEndpointDataRepository;
import com.connectclub.jvbuster.repository.i.JvbInstanceDataRepository;
import com.connectclub.jvbuster.utils.MDCCopyHelper;
import com.connectclub.jvbuster.utils.MethodSync;
import com.connectclub.jvbuster.videobridge.data.Answer;
import com.connectclub.jvbuster.videobridge.data.PrevOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import com.connectclub.jvbuster.videobridge.data.sdp.SessionDescription;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.connectclub.jvbuster.videobridge.i.VideobridgeService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefaultVideobridgeService implements VideobridgeService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final RedissonClient redissonClient;
    private final JvbConferenceDataRepository jvbConferenceDataRepository;
    private final JvbInstanceDataRepository jvbInstanceDataRepository;
    private final JvbConferencesTasksService jvbConferencesTasksService;
    private final JvbEndpointDataRepository jvbEndpointDataRepository;

    private final OkHttpClient okHttpClient;

    public DefaultVideobridgeService(
            RedissonClient redissonClient,
            JvbConferenceDataRepository jvbConferenceDataRepository,
            JvbInstanceDataRepository jvbInstanceDataRepository,
            JvbConferencesTasksService jvbConferencesTasksService,
            JvbEndpointDataRepository jvbEndpointDataRepository,
            OkHttpClient okHttpClient
    ) {
        this.redissonClient = redissonClient;
        this.jvbConferenceDataRepository = jvbConferenceDataRepository;
        this.jvbInstanceDataRepository = jvbInstanceDataRepository;
        this.jvbConferencesTasksService = jvbConferencesTasksService;
        this.jvbEndpointDataRepository = jvbEndpointDataRepository;

        this.okHttpClient = okHttpClient;
    }

    private final List<PayloadType> audioPayloadTypes = List.of(
            PayloadType.builder()
                    .id(111)
                    .name("opus")
                    .clockrate(48000)
                    .channels(2)
                    .parameters(Map.of(
                            "minptime", 10,
                            "useinbandfec", 1,
                            "stereo", 0
                    ))
                    .rtcpFbs(List.of(
                            RtcpFb.builder().type("transport-cc").build()
                    ))
                    .build()
    );

    private final List<RtpHdrext> audioRtpHdrexts = List.of(
            RtpHdrext.builder()
                    .id(1)
                    .uri("urn:ietf:params:rtp-hdrext:ssrc-audio-level")
                    .build(),
            RtpHdrext.builder()
                    .id(5)
                    .uri("http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                    .build()
    );

    private final List<PayloadType> videoPayloadTypes = List.of(
            PayloadType.builder()
                    .id(100)
                    .name("VP8")
                    .clockrate(90000)
                    .parameters(Map.of(
                            "max-fr", 30,
                            "max-recv-width", 480,
                            "max-recv-height", 320
                    ))
                    .rtcpFbs(List.of(
                            RtcpFb.builder().type("ccm").subtype("fir").build(),
                            RtcpFb.builder().type("nack").build(),
                            RtcpFb.builder().type("nack").subtype("pli").build(),
                            RtcpFb.builder().type("transport-cc").build()
                    ))
                    .build(),
            PayloadType.builder()
                    .id(96)
                    .name("rtx")
                    .clockrate(90000)
                    .parameters(Map.of("apt", 100))
                    .rtcpFbs(List.of(
                            RtcpFb.builder().type("ccm").subtype("fir").build(),
                            RtcpFb.builder().type("nack").build(),
                            RtcpFb.builder().type("nack").subtype("pli").build()
                    ))
                    .build()
    );

    private final List<RtpHdrext> videoRtpHdrexts = List.of(
            RtpHdrext.builder()
                    .id(3)
                    .uri("http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time")
                    .build(),
            RtpHdrext.builder()
                    .id(5)
                    .uri("http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                    .build()
    );

    private Content constructAudioContent(String channelId, String endpoint, Channel.Direction direction, List<Long> ssrcs, List<Map.Entry<String, List<Long>>> ssrcGroups) {
        return Content.builder()
                .name("audio")
                .channel(
                        Channel.builder()
                                .id(channelId)
                                .expire(10)
                                .initiator(true)
                                .endpoint(endpoint)
                                .direction(direction)
                                .channelBundleId(endpoint)
                                .rtpLevelRelayType(Channel.RtpLevelRelayType.TRANSLATOR)
                                .lastN(0)
                                .payloadTypes(audioPayloadTypes)
                                .sources(ssrcs)
                                .ssrcGroups(
                                        Stream.ofNullable(ssrcGroups)
                                                .flatMap(Collection::stream)
                                                .map(x -> SsrcGroup.build(x.getKey(), x.getValue()))
                                                .collect(Collectors.toList())
                                )
                                .rtpHdrexts(audioRtpHdrexts)
                                .build()
                )
                .build();

    }

    private Content constructVideoContent(String channelId, String endpoint, Channel.Direction direction, List<Long> ssrcs, List<Map.Entry<String, List<Long>>> ssrcGroups) {
        return Content.builder()
                .name("video")
                .channel(
                        Channel.builder()
                                .id(channelId)
                                .expire(10)
                                .initiator(true)
                                .endpoint(endpoint)
                                .direction(direction)
                                .channelBundleId(endpoint)
                                .rtpLevelRelayType(Channel.RtpLevelRelayType.TRANSLATOR)
                                .lastN(0)
                                .payloadTypes(videoPayloadTypes)
                                .sources(ssrcs)
                                .ssrcGroups(
                                        Stream.ofNullable(ssrcGroups)
                                                .flatMap(Collection::stream)
                                                .map(x -> SsrcGroup.build(x.getKey(), x.getValue()))
                                                .collect(Collectors.toList())
                                )
                                .rtpHdrexts(videoRtpHdrexts)
                                .build()
                )
                .build();
    }

    private Content constructDataContent(String sctpConnectionId, String endpoint) {
        return Content.builder()
                .name("data")
                .sctpConnection(
                        SctpConnection.builder()
                                .id(sctpConnectionId)
                                .expire(10)
                                .initiator(true)
                                .endpoint(endpoint)
                                .port(5000)
                                .channelBundleId(endpoint)
                                .build()
                )
                .build();
    }

    private List<String> getChannelsIds(Conference conference, String contentName, String endpoint) {
        if (conference.getContents() == null) return List.of();
        return conference.getContents().stream()
                .filter(x -> x.getName().equals(contentName))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel)x)
                .filter(x -> x.getEndpoint().equals(endpoint))
                .map(Channel::getId)
                .collect(Collectors.toList());
    }

    private List<String> getSctpConnectionsIds(Conference conference, String endpoint) {
        if (conference.getContents() == null) return List.of();
        return conference.getContents().stream()
                .filter(x -> "data".equals(x.getName()))
                .map(Content::getSctpConnections)
                .flatMap(Collection::stream)
                .filter(x -> x.getEndpoint().equals(endpoint))
                .map(SctpConnection::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SessionDescription> getOffers(String conferenceGid, String endpoint, Long videoBandwidth, String... videobridgeIds) throws IOException, JvbInstanceRestException {
        delete(null, endpoint, true, videobridgeIds);
        Set<String> videobridgeIdSet = Set.of(videobridgeIds);

        RReadWriteLock conferenceGlobalReadWriteLock = redissonClient.getReadWriteLock("conference-global");
        conferenceGlobalReadWriteLock.readLock().lock();
        try {
            List<JvbConferenceData> jvbConferences = jvbConferenceDataRepository.findAllByGid(conferenceGid);

            if (jvbConferences.size() == 0) {
                RLock createConferenceLock = redissonClient.getFairLock("create-conference-" + conferenceGid);
                createConferenceLock.lock();
                try {
                    jvbConferences = jvbConferenceDataRepository.findAllByGid(conferenceGid);
                    if (jvbConferences.size() == 0) {
                        if (videobridgeIds.length > 0) {
                            throw new RuntimeException("Can not get offer from secondary videobridge because primary is not chosen");
                        }
                        JvbInstanceData primaryJvbInstance = jvbInstanceDataRepository
                                .findFirstByRespondingIsTrueAndScheduledForRemovalIsFalseAndShutdownInProgressIsFalseAndForSpeakersOrderByUtilization(true)
                                .orElseThrow(() -> new RuntimeException("Can not choose primary jvb instance"));
                        Conference conf = JvbInstance.from(primaryJvbInstance, okHttpClient).createConference(conferenceGid);
                        JvbConferenceData jvbConference = JvbConferenceData.builder()
                                .id(JvbConferenceData.buildId(conferenceGid, conf.getId()))
                                .gid(conferenceGid)
                                .confId(conf.getId())
                                .instance(primaryJvbInstance)
                                .build();
                        jvbConferenceDataRepository.save(jvbConference);
                        jvbConferences = List.of(jvbConference);
                        log.info("JVB instance(id={}) has been chosen to place the new conference(id={})", primaryJvbInstance.getId(), jvbConference.getId());
                    }
                } finally {
                    createConferenceLock.unlock();
                }
            }

            JvbInstanceData primaryJvbInstance;

            List<String> instanceIds = jvbConferences.stream()
                    .map(JvbConferenceData::getInstance)
                    .map(JvbInstanceData::getId)
                    .collect(Collectors.toList());
            List<JvbInstanceData> jvbInstances = jvbInstanceDataRepository.findAllByIdInOrderByUtilization(instanceIds);
            if (videobridgeIds.length == 0) {
                primaryJvbInstance = jvbInstances.get(0);
            } else {
                //if videobridge ids specified then they are all for shadow endpoints, so we do not choose videobridge to be as primary node
                primaryJvbInstance = null;
            }
            if (primaryJvbInstance != null) {
                log.info("JVB instance(id={}) has been chosen as primary for endpoint", primaryJvbInstance.getId());
                if (primaryJvbInstance.getCpuLoad() != null && primaryJvbInstance.getCpuLoad() > 0.9) {
                    throw new RuntimeException("JVB instance CPU load is above 90%");
                }
            }

            return jvbInstances.stream()
                    .filter(x -> videobridgeIdSet.size() == 0 || videobridgeIdSet.contains(x.getId()))
                    .map(jvbInstance -> {
                        try {
                            JvbConferenceData jvbConference = jvbConferenceDataRepository.findByGidAndInstanceId(conferenceGid, jvbInstance.getId())
                                    .orElseThrow(() -> new NoSuchElementException(String.format("Can not find conference(gid=%s, instanceId=%s)", conferenceGid, jvbInstance.getId())));
                            boolean primary = jvbInstance == primaryJvbInstance;
                            Conference conferenceWithNewChannels = Conference.builder()
                                    .id(jvbConference.getConfId())
                                    .gid(jvbConference.getGid())
                                    .contents(List.of(
                                            constructAudioContent(null, endpoint, primary ? Channel.Direction.SENDRECV : Channel.Direction.SENDONLY, null, null),
                                            constructVideoContent(null, endpoint, primary ? Channel.Direction.SENDRECV : Channel.Direction.SENDONLY, null, null),
                                            constructDataContent(null, endpoint)
                                    ))
                                    .endpoints(List.of(Endpoint.builder()
                                            .id(endpoint)
                                            .displayName(primary ? "primary" : "shadow")
                                            .build()))
                                    .channelBundles(List.of(
                                            ChannelBundle.builder()
                                                    .id(endpoint)
                                                    .transport(
                                                            Transport.builder()
                                                                    .xmlns("urn:xmpp:jingle:transports:ice-udp:1")
                                                                    .rtcpMux(true)
                                                                    .build()
                                                    )
                                                    .build()
                                    ))
                                    .build();
                            return JvbInstance.from(jvbInstance, okHttpClient).patchConference(conferenceWithNewChannels);
                        } catch (Exception e) {
                            log.error("Create endpoint exception", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(x -> SdpUtils.toOfferSdp(x, endpoint, videoBandwidth, null))
                    .collect(Collectors.toList());
        } finally {
            conferenceGlobalReadWriteLock.readLock().unlock();
        }
    }

    @SneakyThrows
    private Conference getConference(JvbConferenceData jvbConferenceData) {
        JvbInstanceData jvbInstanceData = jvbConferenceData.getInstance();
        return JvbInstance.from(jvbInstanceData, okHttpClient).getConference(jvbConferenceData.getConfId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionDescription> getOffers(String conferenceGid, String endpoint, Long videoBandwidth, List<PrevOffer> prevOffers) {
        return prevOffers.stream()
                .map(prevOffer -> jvbConferenceDataRepository.findById(JvbConferenceData.buildId(conferenceGid, prevOffer.getConferenceId()))
                        .map(this::getConference)
                        .map(conf -> SdpUtils.toOfferSdp(conf, endpoint, videoBandwidth, prevOffer))
                        .orElseThrow(() -> new NoSuchElementException(String.format("Can not find conference(id=%s) in cache", JvbConferenceData.buildId(conferenceGid, prevOffer.getConferenceId())))))
                .collect(Collectors.toList());
    }

    @Override
    @MethodSync(lockName = "conference-global", mode = MethodSync.Mode.READ)
    @Transactional(readOnly = true)
    public void processAnswers(String conferenceGid, String endpoint, List<Answer> answers) throws IOException, JvbInstanceRestException {
        for (Answer answer : answers) {
            JvbConferenceData conferenceData = jvbConferenceDataRepository.findById(JvbConferenceData.buildId(conferenceGid, answer.getConferenceId()))
                    .orElseThrow(() -> new NoSuchElementException(String.format("Can not find conference(id=%s)", JvbConferenceData.buildId(conferenceGid, answer.getConferenceId()))));

            List<Content> contents = new ArrayList<>();
            contents.add(constructDataContent(answer.getSctpConnectionId(), endpoint));
            if (answer.getAudioChannelId() != null && answer.getAudioSsrc() != null) {
                contents.add(constructAudioContent(answer.getAudioChannelId(), endpoint, Channel.Direction.SENDRECV, answer.getAudioSsrc(), answer.getAudioSsrcGroups()));
            }

            if (answer.getVideoChannelId() != null && answer.getVideoSsrc() != null) {
                contents.add(constructVideoContent(answer.getVideoChannelId(), endpoint, Channel.Direction.SENDRECV, answer.getVideoSsrc(), answer.getVideoSsrcGroups()));
            }

            Conference conferenceUpdate = Conference.builder()
                    .id(conferenceData.getConfId())
                    .gid(conferenceData.getGid())
                    .contents(contents)
                    .channelBundles(List.of(
                            ChannelBundle.builder()
                                    .id(endpoint)
                                    .transport(
                                            Transport.builder()
                                                    .xmlns("urn:xmpp:jingle:transports:ice-udp:1")
                                                    .rtcpMux(true)
                                                    .ufrag(answer.getIceUfrag())
                                                    .pwd(answer.getIcePwd())
                                                    .fingerprints(
                                                            List.of(
                                                                    Fingerprint.builder()
                                                                            .fingerprint(answer.getFingerprintValue())
                                                                            .hash(answer.getFingerprintHash())
                                                                            .setup(answer.getFingerprintSetup())
                                                                            .build()
                                                            ))
                                                    .candidates(answer.getCandidates().stream()
                                                            .map(x->Candidate.builder()
                                                                    .generation(x.getGeneration())
                                                                    .ip(x.getConnectionAddress())
                                                                    .port(Integer.parseInt(x.getPort()))
                                                                    .component(Integer.parseInt(x.getComponentId()))
                                                                    .protocol(gson.fromJson(x.getTransport(), Candidate.Protocol.class))
                                                                    .type(gson.fromJson(x.getCandidateType(), Candidate.Type.class))
                                                                    .foundation(x.getFoundation())
                                                                    .priority(Integer.parseInt(x.getPriority()))
                                                                    .relAddr(x.getRelAddress())
                                                                    .relPort(x.getRelPort() == null ? -1 : Integer.parseInt(x.getRelPort()))
                                                                    .build())
                                                            .collect(Collectors.toList()))
                                                    .build()
                                    )
                                    .build()
                    ))
                    .build();
            JvbInstanceData jvbInstanceData = conferenceData.getInstance();
            JvbInstance.from(jvbInstanceData, okHttpClient).patchConference(conferenceUpdate);
        }
        jvbConferencesTasksService.syncConferences(conferenceGid);
    }

    @Override
    public void processIceCandidate(
            String conferenceGid,
            String endpoint,
            String conferenceId,
            String foundation,
            int component,
            Candidate.Protocol protocol,
            int priority,
            String ip,
            int port,
            Candidate.Type type,
            String relAddr,
            Integer relPort,
            String generation
    ) {

    }

    @Override
    @Transactional
    public void delete(String conferenceGid, String endpoint, boolean quiet, String... videobridgeIds) {
        JvbEndpointData jvbEndpoint = jvbEndpointDataRepository.findById(endpoint).orElse(null);

        if (jvbEndpoint == null) {
            if (quiet) return;
            throw new EndpointNotFound();
        }

        String endpointConfGid = jvbEndpoint.getConference().getGid();

        if (conferenceGid != null && !Objects.equals(conferenceGid, endpointConfGid)) {
            if (quiet) return;
            throw new EndpointNotFound(
                    String.format("Endpoint with the same id exists but in different conference(%s)", endpointConfGid)
            );
        }

        List<JvbConferenceData> conferenceDataList = videobridgeIds.length == 0
                ? jvbConferenceDataRepository.findAllByGid(endpointConfGid)
                : jvbConferenceDataRepository.findAllByGidAndInstanceIdIn(endpointConfGid, List.of(videobridgeIds));

        MDCCopyHelper mdcCopyHelper = new MDCCopyHelper();
        conferenceDataList.parallelStream()
                .peek(mdcCopyHelper::set)
                .forEach(jvbConferenceData -> {
                    try {
                        JvbInstance jvbInstance = JvbInstance.from(jvbConferenceData.getInstance(), okHttpClient);
                        try {
                            jvbInstance.deleteEndpoint(jvbConferenceData.getConfId(), endpoint);
                        } catch (Exception e) {
                            log.error("Remove endpoint failed(jvbInstance={}, conferenceId={})", jvbInstance, jvbConferenceData.getConfId(), e);
                        }
                    } finally {
                        mdcCopyHelper.clear();
                    }
                });
        jvbEndpointDataRepository.delete(jvbEndpoint);
    }
}
