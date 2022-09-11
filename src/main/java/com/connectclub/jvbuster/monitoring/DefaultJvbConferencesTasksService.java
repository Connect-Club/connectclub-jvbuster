package com.connectclub.jvbuster.monitoring;

import com.connectclub.jvbuster.monitoring.i.JvbConferencesTasksService;
import com.connectclub.jvbuster.repository.data.JvbConferenceData;
import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import com.connectclub.jvbuster.repository.i.JvbConferenceDataRepository;
import com.connectclub.jvbuster.repository.i.JvbInstanceDataRepository;
import com.connectclub.jvbuster.utils.MDCCopyHelper;
import com.connectclub.jvbuster.utils.MethodSync;
import com.connectclub.jvbuster.utils.MethodSyncArg;
import com.connectclub.jvbuster.videobridge.JvbConferenceUtils;
import com.connectclub.jvbuster.videobridge.JvbInstance;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefaultJvbConferencesTasksService implements JvbConferencesTasksService {

    public static final String AUDIO_MIXER_EP_ID = "1";

    private final JvbConferenceDataRepository jvbConferenceDataRepository;
    private final JvbInstanceDataRepository jvbInstanceDataRepository;
    private final int jvbInstanceSpeakerMaxUtilization;
    private final int jvbInstanceListenerMaxUtilization;

    private final OkHttpClient okHttpClient;

    public DefaultJvbConferencesTasksService(
            @Value("${jvb.conference.instance-max-utilization.speaker}") int jvbInstanceSpeakerMaxUtilization,
            @Value("${jvb.conference.instance-max-utilization.listener}") int jvbInstanceListenerMaxUtilization,
            JvbConferenceDataRepository jvbConferenceDataRepository,
            JvbInstanceDataRepository jvbInstanceDataRepository,
            OkHttpClient okHttpClient
    ) {
        this.jvbInstanceSpeakerMaxUtilization = jvbInstanceSpeakerMaxUtilization;
        this.jvbInstanceListenerMaxUtilization = jvbInstanceListenerMaxUtilization;

        this.jvbConferenceDataRepository = jvbConferenceDataRepository;
        this.jvbInstanceDataRepository = jvbInstanceDataRepository;

        this.okHttpClient = okHttpClient;
    }

    private List<Conference> getConferencesFromInstance(JvbInstance jvbInstance) {
        try {
            return jvbInstance.getConferences();
        } catch (Exception e) {
            log.warn("Can not get conferences from instance(id={})", jvbInstance.getId(), e);
            return List.of();
        }
    }

    @Override
    @MethodSync(lockName = "conference-global", mode = MethodSync.Mode.WRITE)
    @Transactional
    public void cacheConferences() {
        List<JvbInstanceData> liveInstances = jvbInstanceDataRepository.findAllByRespondingIsTrue();

        MDCCopyHelper mdcCopyHelper = new MDCCopyHelper();
        List<JvbConferenceData> jvbConferenceDataList = liveInstances.parallelStream()
                .peek(mdcCopyHelper::set)
                .map(x -> JvbInstance.from(x, okHttpClient))
                .flatMap(jvbInstance -> getConferencesFromInstance(jvbInstance).stream()
                        .filter(conf -> conf.getGid() != null && !conf.getGid().isBlank())
                        .map(conf -> JvbConferenceData.builder()
                                .id(JvbConferenceData.buildId(conf.getGid(), conf.getId()))
                                .gid(conf.getGid())
                                .confId(conf.getId())
                                .instance(jvbInstanceDataRepository.getOne(jvbInstance.getId()))
                                .build())
                )
                .peek(mdcCopyHelper::clear)
                .collect(Collectors.toList());

        List<String> ids = jvbConferenceDataList.stream()
                .map(JvbConferenceData::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            jvbConferenceDataRepository.deleteAll();
        } else {
            jvbConferenceDataRepository.deleteAllByIdNotIn(ids);
        }
        jvbConferenceDataRepository.saveAll(jvbConferenceDataList);
    }

    @Override
    @Transactional
    public void scaleConferences(boolean forSpeakers) {
        JvbInstanceData newInstanceCandidate = jvbInstanceDataRepository
                .findFirstByRespondingIsTrueAndScheduledForRemovalIsFalseAndShutdownInProgressIsFalseAndForSpeakersOrderByUtilization(forSpeakers)
                .orElse(null);
        jvbConferenceDataRepository.findAllByInstanceForSpeakers(forSpeakers).stream()
                .collect(Collectors.groupingBy(JvbConferenceData::getGid))
                .forEach((gid, jvbConferenceDataList) -> {
                    try {
                        List<JvbInstanceData> jvbInstanceDataList = jvbConferenceDataList.stream()
                                .map(JvbConferenceData::getInstance)
                                .collect(Collectors.toList());
                        int jvbInstanceMaxUtilization = forSpeakers ? jvbInstanceSpeakerMaxUtilization : jvbInstanceListenerMaxUtilization;
                        boolean needNewInstance = jvbInstanceDataList.stream()
                                .mapToInt(JvbInstanceData::getUtilization)
                                .allMatch(x -> x >= jvbInstanceMaxUtilization);
                        if (needNewInstance) {
                            if (newInstanceCandidate == null || jvbConferenceDataList.stream().anyMatch(x -> Objects.equals(x.getInstance(), newInstanceCandidate))) {
                                log.warn("Conference(gid={}) need new JVB instance but there is no suitable candidate for expansion", gid);
                            } else {
                                log.info("Conference(gid={}) will be expand on JVB instance(id={})", gid, newInstanceCandidate.getId());
                                Conference newConference = JvbInstance.from(newInstanceCandidate, okHttpClient).createConference(gid);
                                jvbConferenceDataRepository.save(
                                        JvbConferenceData.builder()
                                                .id(JvbConferenceData.buildId(gid, newConference.getId()))
                                                .gid(gid)
                                                .confId(newConference.getId())
                                                .instance(newInstanceCandidate)
                                                .build()
                                );
                                if (forSpeakers) {
                                    jvbInstanceDataList.stream()
                                            .map(x -> JvbInstance.from(x, okHttpClient))
                                            .forEach(instance -> {
                                                String confId = jvbConferenceDataList.stream()
                                                        .filter(conf -> Objects.equals(conf.getInstance().getId(), instance.getId()))
                                                        .map(JvbConferenceData::getConfId)
                                                        .findFirst()
                                                        .orElseThrow(() -> new NoSuchElementException(String.format("Can not find conference(instanceId=%s) in cache=%s", instance.getId(), jvbConferenceDataList)));
                                                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                                    @Override
                                                    public void afterCommit() {
                                                        try {
                                                            instance.broadcastMessage(confId, "NewVideobridgeAddedToConference", Map.of("videobridgeId", newInstanceCandidate.getId()));
                                                            log.info("`NewVideobridgeAddedToConference` message for conference(confId={}) has been broadcasted on JVB instance(id={})", confId, instance.getId());
                                                        } catch (Exception e) {
                                                            log.error("Broadcast `NewVideobridgeAddedToConference` message error. instanceId={},confId={}", instance.getId(), confId, e);
                                                        }
                                                    }
                                                });
                                            });
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("", e);
                    }
                });
    }

    @Override
    @MethodSync(lockName = "conference-global", mode = MethodSync.Mode.WRITE)
    @Transactional
    public void expireConferences() {
        List<JvbInstanceData> liveInstances = jvbInstanceDataRepository.findAllByRespondingIsTrue();

        Set<String> expiredConfGid = new HashSet<>();

        liveInstances.forEach(instanceData -> {
            try {
                List<Conference> expiredConferences = JvbInstance.from(instanceData, okHttpClient).expireConferences();
                expiredConferences.forEach(conf -> jvbConferenceDataRepository.deleteById(JvbConferenceData.buildId(conf.getGid(), conf.getId())));
                expiredConfGid.addAll(expiredConferences.stream().map(Conference::getGid).collect(Collectors.toList()));
            } catch (Exception e) {
                log.error("Error when expire conferences on instance(id={}, host={}))", instanceData.getId(), instanceData.getHost(), e);
            }
        });
        expiredConfGid.forEach(this::syncConferences);
    }

    private Conference getConference(JvbConferenceData jvbConferenceData) {
        JvbInstanceData jvbInstanceData = jvbConferenceData.getInstance();
        try {
            return JvbInstance.from(jvbInstanceData, okHttpClient).getConference(jvbConferenceData.getConfId());
        } catch (Exception e) {
            log.error("Error when get conference", e);
            return null;
        }
    }

    @Override
    @MethodSync(lockName = "sync-conference")
    @Transactional
    public void syncConferences(@MethodSyncArg String gid) {
        try {
            Long.parseLong(gid, 16);
        } catch (NumberFormatException e) {
            return;
        }
        List<JvbConferenceData> conferencesDataForSpeakers = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(gid, true);
        List<JvbConferenceData> conferencesDataForListeners = jvbConferenceDataRepository.findAllByGidAndInstanceForSpeakers(gid, false);
        if (conferencesDataForSpeakers.size() == 0 && conferencesDataForListeners.size() == 0) return;
        List<Conference> conferencesForSpeakers = conferencesDataForSpeakers.stream()
                .map(this::getConference)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Content> speakersContents = conferencesForSpeakers.stream()
                .map(Conference::getContents)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Map<String, UUID> endpoints = conferencesForSpeakers.stream()
                .map(Conference::getEndpoints)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Endpoint::getId, Endpoint::getUuid));
        endpoints.put(AUDIO_MIXER_EP_ID, new UUID(0L, 0L));

        conferencesDataForListeners.forEach(conf -> {
            Conference patchWithOcto = JvbConferenceUtils.buildPatchWithOcto(
                    gid,
                    conf.getConfId(),
                    conferencesDataForSpeakers.stream()
                            .map(JvbConferenceData::getInstance)
                            .map(x -> x.getHost() + ":" + x.getOctoBindPort())
                            .collect(Collectors.toSet()),
                    endpoints,
                    Stream.concat(
                            Stream.of(Map.entry(AUDIO_MIXER_EP_ID, List.of(0L))),
                            speakersContents.stream()
                                    .filter(x -> "audio".equals(x.getName()))
                                    .flatMap(x -> x.getChannels().stream())
                                    .filter(x -> x instanceof Channel)
                                    .map(Channel.class::cast)
                                    .filter(x -> x.getSsrcs() != null && x.getSsrcs().size() > 0)
                                    .map(x -> Map.entry(x.getEndpoint(), x.getSsrcs()))
                    ).collect(Collectors.toList()),
                    speakersContents.stream()
                            .filter(x -> "video".equals(x.getName()))
                            .flatMap(x -> x.getChannels().stream())
                            .filter(x -> x instanceof Channel)
                            .map(Channel.class::cast)
                            .filter(x -> x.getSsrcs() != null && x.getSsrcs().size() > 0)
                            .map(x -> Map.entry(x.getEndpoint(), x.getSsrcs()))
                            .collect(Collectors.toList()),
                    speakersContents.stream()
                            .filter(x -> "video".equals(x.getName()))
                            .flatMap(x -> x.getChannels().stream())
                            .filter(x -> x instanceof Channel)
                            .map(Channel.class::cast)
                            .map(ChannelCommon::getSsrcGroups)
                            .flatMap(Collection::stream)
                            .filter(x -> x.getSources() != null && x.getSources().size() > 0)
                            .map(x -> Map.entry(x.getSemantics(), x.getSources()))
                            .collect(Collectors.toList())
            );
            try {
                JvbInstance.from(conf.getInstance(), okHttpClient).patchConference(patchWithOcto);
            } catch (Exception e) {
                log.error("Error when patch octo endpoints for listeners", e);
            }
        });
        conferencesDataForSpeakers.forEach(conf -> {
            Conference patchWithOcto = JvbConferenceUtils.buildPatchWithOcto(
                    gid,
                    conf.getConfId(),
                    conferencesDataForListeners.stream()
                            .map(JvbConferenceData::getInstance)
                            .map(x -> x.getHost() + ":" + x.getOctoBindPort())
                            .collect(Collectors.toSet()),
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
            try {
                JvbInstance.from(conf.getInstance(), okHttpClient).patchConference(patchWithOcto);
            } catch (Exception e) {
                log.error("Error when patch octo endpoints for speakers", e);
            }
        });
    }
}
