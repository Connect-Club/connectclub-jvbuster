package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.exception.ConflictException;
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
import com.connectclub.jvbuster.videobridge.data.VideobridgeConferenceOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.Channel;
import com.connectclub.jvbuster.videobridge.data.jvb.Conference;
import com.connectclub.jvbuster.videobridge.data.jvb.Endpoint;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.connectclub.jvbuster.videobridge.i.NewVideobridgeService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultNewVideobridgeService implements NewVideobridgeService {

    private final RedissonClient redissonClient;
    private final JvbConferenceDataRepository jvbConferenceDataRepository;
    private final JvbInstanceDataRepository jvbInstanceDataRepository;
    private final JvbConferencesTasksService jvbConferencesTasksService;
    private final JvbEndpointDataRepository jvbEndpointDataRepository;

    private final TransactionTemplate transactionTemplate;
    private final OkHttpClient okHttpClient;

    public DefaultNewVideobridgeService(
            RedissonClient redissonClient,
            JvbConferenceDataRepository jvbConferenceDataRepository,
            JvbInstanceDataRepository jvbInstanceDataRepository,
            JvbConferencesTasksService jvbConferencesTasksService,
            JvbEndpointDataRepository jvbEndpointDataRepository,
            TransactionTemplate transactionTemplate,
            OkHttpClient okHttpClient
    ) {
        this.redissonClient = redissonClient;
        this.jvbConferenceDataRepository = jvbConferenceDataRepository;
        this.jvbInstanceDataRepository = jvbInstanceDataRepository;
        this.jvbConferencesTasksService = jvbConferencesTasksService;
        this.jvbEndpointDataRepository = jvbEndpointDataRepository;

        this.transactionTemplate = transactionTemplate;
        this.okHttpClient = okHttpClient;
    }

    @Override
    @MethodSync(lockName = "conference-global", mode = MethodSync.Mode.READ)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<VideobridgeConferenceOffer> getNewOffers(String conferenceGid, String endpoint, boolean speaker) throws IOException, JvbInstanceRestException {
        if (!speaker) {
            try {
                Long.parseLong(conferenceGid, 16);
            } catch (NumberFormatException e) {
                throw new RuntimeException("listeners supported only for conference with gid parseable to long type");
            }
        }

        delete(null, endpoint, true);

        RLock createConferenceLock = redissonClient.getFairLock("create-conference-" + conferenceGid);
        createConferenceLock.lock();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    List<JvbConferenceData> jvbConferences = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(conferenceGid, speaker);
                    if (jvbConferences.size() == 0) {
                        if (endpoint.startsWith("screen-")) {
                            throw new ConflictException("Screen`s endpoint can not create conference");
                        }
                        JvbInstanceData primaryJvbInstance = jvbInstanceDataRepository
                                .findFirstByRespondingIsTrueAndScheduledForRemovalIsFalseAndShutdownInProgressIsFalseAndForSpeakersOrderByUtilization(speaker)
                                .orElseThrow(() -> new RuntimeException("Can not choose primary jvb instance"));
                        Conference conf = JvbInstance.from(primaryJvbInstance, okHttpClient).createConference(conferenceGid);
                        JvbConferenceData jvbConference = JvbConferenceData.builder()
                                .id(JvbConferenceData.buildId(conferenceGid, conf.getId()))
                                .gid(conferenceGid)
                                .confId(conf.getId())
                                .instance(primaryJvbInstance)
                                .build();
                        jvbConferenceDataRepository.save(jvbConference);
                        log.info("JVB instance(id={}) has been chosen to place the new conference(id={}, forSpeakers={})", primaryJvbInstance.getId(), jvbConference.getId(), speaker);
                        if (speaker) {
                            Conference patchWithAudioMixer = JvbConferenceUtils.buildPatchWithAudioMixer(conferenceGid, conf.getId());
                            JvbInstance.from(primaryJvbInstance, okHttpClient).patchConference(patchWithAudioMixer);
                        }
                        jvbConferencesTasksService.syncConferences(conferenceGid);
                    }
                } catch (IOException | JvbInstanceRestException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            createConferenceLock.unlock();
        }

        List<JvbConferenceData> jvbConferences = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(conferenceGid, speaker);

        if (jvbConferences.size() == 0) {
            throw new RuntimeException("No conferences to choose from");
        }

        JvbConferenceData primaryJvbConference = jvbConferences.stream()
                .min(Comparator.comparingInt(x -> x.getInstance().getUtilization()))
                .orElseThrow();
        log.info("JVB instance(id={}) has been chosen as primary for endpoint", primaryJvbConference.getInstance().getId());
        if (primaryJvbConference.getInstance().getCpuLoad() != null && primaryJvbConference.getInstance().getCpuLoad() > 0.9) {
            throw new RuntimeException("JVB instance CPU load is above 90%");
        }

        List<VideobridgeConferenceOffer> result;

        if (speaker) {
            MDCCopyHelper mdcCopyHelper = new MDCCopyHelper();
            result = jvbConferences.parallelStream()
                    .peek(mdcCopyHelper::set)
                    .map(jvbConference -> {
                        try {
                            boolean primary = jvbConference == primaryJvbConference;
                            JvbInstanceData jvbInstance = jvbConference.getInstance();
                            Conference conferenceWithNewChannels = JvbConferenceUtils.buildPatchWithNewEndpoint(
                                    jvbConference,
                                    endpoint,
                                    primary ? "primary" : "shadow",
                                    primary ? Channel.Direction.SENDRECV : Channel.Direction.SENDONLY
                            );
                            return Map.entry(
                                    jvbInstance.getId(),
                                    JvbInstance.from(jvbInstance, okHttpClient).patchConference(conferenceWithNewChannels)
                            );
                        } catch (Exception e) {
                            log.error("Create endpoint exception", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .map(x -> VideobridgeConferenceUtils.toVideobridgeConference(x.getKey(), x.getValue(), endpoint))
                    .peek(mdcCopyHelper::clear)
                    .collect(Collectors.toList());
        } else {
            Conference conferenceWithNewChannels = JvbConferenceUtils.buildPatchWithNewEndpoint(
                    primaryJvbConference,
                    endpoint,
                    "primary",
                    Channel.Direction.SENDONLY
            );
            conferenceWithNewChannels = JvbInstance.from(primaryJvbConference.getInstance(), okHttpClient).patchConference(conferenceWithNewChannels, endpoint);
            List<Conference> conferencesForSpeakers = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(conferenceGid, true).stream()
                    .map(this::getConference)
                    .collect(Collectors.toList());
            result = List.of(
                    VideobridgeConferenceUtils.toVideobridgeConference(
                            primaryJvbConference.getInstance().getId(),
                            conferenceWithNewChannels,
                            conferencesForSpeakers,
                            endpoint
                    )
            );
        }
        jvbEndpointDataRepository.save(
                JvbEndpointData.builder()
                        .id(endpoint)
                        .conference(primaryJvbConference)
                        .speaker(speaker)
                        .build()
        );
        return result;
    }

    @SneakyThrows
    private Conference getConference(JvbConferenceData jvbConferenceData) {
        JvbInstanceData jvbInstanceData = jvbConferenceData.getInstance();
        return JvbInstance.from(jvbInstanceData, okHttpClient).getConference(jvbConferenceData.getConfId());
    }

    @SneakyThrows
    private Conference getConferenceWithOnlyOneEndpoint(JvbConferenceData jvbConferenceData, String endpoint) {
        JvbInstanceData jvbInstanceData = jvbConferenceData.getInstance();
        return JvbInstance.from(jvbInstanceData, okHttpClient).getConference(jvbConferenceData.getConfId(), endpoint);
    }

    private static Endpoint findEndpoint(Conference conference, String endpoint) {
        return conference.getEndpoints().stream()
                .filter(x -> Objects.equals(x.getId(), endpoint))
                .findFirst().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideobridgeConferenceOffer> getCurrentOffers(String conferenceGid, String endpoint) throws JvbInstanceRestException, IOException {
        JvbEndpointData jvbEndpoint = jvbEndpointDataRepository.findById(endpoint).orElseThrow(EndpointNotFound::new);
        List<VideobridgeConferenceOffer> result = new ArrayList<>();

        if (jvbEndpoint.isSpeaker()) {
            Map<JvbConferenceData, Conference> jvbConferenceDataMap = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(conferenceGid, true).stream()
                    .collect(Collectors.toMap(x -> x, this::getConference));

            Set<Conference> conferencesWithEndpoint = jvbConferenceDataMap.values().stream()
                    .filter(x->findEndpoint(x, endpoint) != null)
                    .collect(Collectors.toSet());
            if (conferencesWithEndpoint.size() == 0) {
                throw new EndpointNotFound();
            }

            for (JvbConferenceData jvbConferenceData : jvbConferenceDataMap.keySet()) {
                Conference conference = jvbConferenceDataMap.get(jvbConferenceData);
                if (!conferencesWithEndpoint.contains(conference)) {
                    Conference conferencePatch = JvbConferenceUtils.buildPatchWithNewEndpoint(jvbConferenceData, endpoint, "shadow", Channel.Direction.SENDONLY);
                    conference = JvbInstance.from(jvbConferenceData.getInstance(), okHttpClient).patchConference(conferencePatch);
                }
                result.add(VideobridgeConferenceUtils.toVideobridgeConference(jvbConferenceData.getInstance().getId(), conference, endpoint));
            }
        } else {
            List<Conference> conferencesForSpeakers = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(conferenceGid, true).stream()
                    .map(this::getConference)
                    .collect(Collectors.toList());

            result.add(VideobridgeConferenceUtils.toVideobridgeConference(
                    jvbEndpoint.getConference().getInstance().getId(),
                    getConferenceWithOnlyOneEndpoint(jvbEndpoint.getConference(), endpoint),
                    conferencesForSpeakers,
                    endpoint
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void processAnswers(String conferenceGid, String endpoint, List<VideobridgeConferenceAnswer> conferences) throws IOException, JvbInstanceRestException {
        for (VideobridgeConferenceAnswer conference : conferences) {
            Conference conferencePatch = JvbConferenceUtils.buildPatchBasedOnAnswer(conferenceGid, endpoint, conference);
            JvbInstanceData jvbInstanceData = jvbInstanceDataRepository.findById(conference.getVideobridgeId())
                    .orElseThrow(() -> new NoSuchElementException(String.format("Can not find instance(id=%s)", conference.getVideobridgeId())));
            JvbInstance.from(jvbInstanceData, okHttpClient).patchConference(conferencePatch);
        }
        jvbConferencesTasksService.syncConferences(conferenceGid);
    }

    @Override
    @Transactional
    public void delete(String conferenceGid, String endpoint, boolean quiet) {
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

        // There may be shadow endpoints, so we can not just use only one conference from endpoint
        List<JvbConferenceData> conferenceDataList = jvbConferenceDataRepository.findAllByGid(endpointConfGid);

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
