package com.connectclub.jvbuster.monitoring;

import com.connectclub.jvbuster.monitoring.i.JvbInstancesTasksService;
import com.connectclub.jvbuster.nodeexporter.i.NodeExporterService;
import com.connectclub.jvbuster.repository.data.JvbConferenceData;
import com.connectclub.jvbuster.repository.data.JvbEndpointData;
import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import com.connectclub.jvbuster.repository.i.JvbConferenceDataRepository;
import com.connectclub.jvbuster.repository.i.JvbInstanceDataRepository;
import com.connectclub.jvbuster.utils.MDCCopyHelper;
import com.connectclub.jvbuster.videobridge.JvbInstance;
import com.connectclub.jvbuster.videobridge.data.jvb.Stats;
import com.connectclub.jvbuster.videobridge.i.JvbInstanceManagementService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DefaultJvbInstancesTasksService implements JvbInstancesTasksService {

    private final int jvbMinPoolSize;
    private final int jvbInstanceEndpointsCapacity;

    private final JvbInstanceManagementService jvbInstanceManagementService;
    private final JvbInstanceDataRepository jvbInstanceDataRepository;
    private final JvbConferenceDataRepository jvbConferenceDataRepository;
    private final NodeExporterService nodeExporterService;
    private final int jvbInstanceMaxUtilization;
    private final int jvbInstanceMinUtilization;
    private final int jvbInstanceLifetimeInMinutes;

    private final OkHttpClient okHttpClient;

    public DefaultJvbInstancesTasksService(
            @Value("${jvb.min.pool.size}") int jvbMinPoolSize,
            @Value("${jvb.machine.endpoints.capacity}") int jvbInstanceEndpointsCapacity,
            @Value("${jvb.machine.max-utilization}") int jvbInstanceMaxUtilization,
            @Value("${jvb.machine.min-utilization}") int jvbInstanceMinUtilization,
            @Value("${jvb.machine.lifetime-in-minutes}") int jvbInstanceLifetimeInMinutes,
            JvbInstanceManagementService jvbInstanceManagementService,
            JvbInstanceDataRepository jvbInstanceDataRepository,
            JvbConferenceDataRepository jvbConferenceDataRepository,
            NodeExporterService nodeExporterService,
            OkHttpClient okHttpClient
    ) {
        this.jvbMinPoolSize = jvbMinPoolSize;
        this.jvbInstanceEndpointsCapacity = jvbInstanceEndpointsCapacity;
        this.jvbInstanceMaxUtilization = jvbInstanceMaxUtilization;
        this.jvbInstanceMinUtilization = jvbInstanceMinUtilization;
        this.jvbInstanceLifetimeInMinutes = jvbInstanceLifetimeInMinutes;

        this.jvbInstanceManagementService = jvbInstanceManagementService;

        this.jvbInstanceDataRepository = jvbInstanceDataRepository;
        this.jvbConferenceDataRepository = jvbConferenceDataRepository;

        this.nodeExporterService = nodeExporterService;

        this.okHttpClient = okHttpClient;
    }

    private boolean needShutdown(JvbInstance jvbInstance) {
        if (!Objects.equals(jvbInstance.getVersion(), jvbInstanceManagementService.getLastVersion())) {
            return true;
        }
        if (jvbInstance.getCreationTimestamp() != null && jvbInstance.getCreationTimestamp().plus(jvbInstanceLifetimeInMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void cacheInstances() {
        List<JvbInstance> activeInstances;
        try {
            activeInstances = jvbInstanceManagementService.getActive();
        } catch (Exception e) {
            log.error("Can not get active JVB instances", e);
            return;
        }

        List<String> activeInstancesIds = activeInstances.stream()
                .map(JvbInstance::getId)
                .collect(Collectors.toList());

        Map<String, JvbInstanceData> prevJvbInstanceDatas = jvbInstanceDataRepository.findAllById(activeInstancesIds).stream()
                .collect(Collectors.toMap(JvbInstanceData::getId, x -> x));
        List<String> instancesIdsForClearConferences = new CopyOnWriteArrayList<>();

        List<JvbInstanceData> jvbInstanceDataList = new CopyOnWriteArrayList<>();
        MDCCopyHelper mdcCopyHelper = new MDCCopyHelper();
        activeInstances.parallelStream()
                .peek(mdcCopyHelper::set)
                .forEach(jvbInstance -> {
                    try {
                        Optional<JvbInstanceData> prevJvbInstanceDataOptional = Optional.ofNullable(prevJvbInstanceDatas.get(jvbInstance.getId()));
                        Stats stats = null;
                        try {
                            //sometimes getConferences() fails but getStats() not
                            jvbInstance.getConferences();
                            stats = jvbInstance.getStats();
                        } catch (Exception e) {
                            if (prevJvbInstanceDataOptional.isPresent() && prevJvbInstanceDataOptional.get().isRespondedOnce()) {
                                log.warn(
                                        "Can not get statistics from JVB instance(id={}). Previous JVB instance data = {}",
                                        jvbInstance.getId(),
                                        prevJvbInstanceDataOptional.orElse(null),
                                        e
                                );
                            }
                        }
                        if (stats == null) {
                            if (prevJvbInstanceDataOptional.map(JvbInstanceData::isResponding).orElse(false)) {
                                instancesIdsForClearConferences.add(jvbInstance.getId());
                            }
                            JvbInstanceData jvbInstanceData = JvbInstanceData.builder()
                                    .id(jvbInstance.getId())
                                    .version(jvbInstance.getVersion())
                                    .scheme(jvbInstance.getScheme())
                                    .host(jvbInstance.getHost())
                                    .port(jvbInstance.getPort())
                                    .forSpeakers(jvbInstance.isForSpeakers())
                                    .octoBindPort(jvbInstance.getOctoBindPort())
                                    .needShutdown(needShutdown(jvbInstance))
                                    .responding(false)
                                    .respondedOnce(prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::isRespondedOnce)
                                            .orElse(false)
                                    )
                                    .scheduledForRemoval(prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::isScheduledForRemoval)
                                            .orElse(false)
                                    )
                                    .shutdownInProgress(prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::isShutdownInProgress)
                                            .orElse(false))
                                    .utilization(prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::getUtilization)
                                            .orElse(null))
                                    .notRespondingSince(prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::getNotRespondingSince)
                                            .orElse(LocalDateTime.now()))
                                    .build();
                            jvbInstanceDataList.add(jvbInstanceData);
                        } else {
                            if (!prevJvbInstanceDataOptional.map(JvbInstanceData::isResponding).orElse(false)) {
                                jvbInstance.deleteConferences();
                            }

                            JvbInstanceData.JvbInstanceDataBuilder jvbInstanceDataBuilder = JvbInstanceData.builder()
                                    .id(jvbInstance.getId())
                                    .version(jvbInstance.getVersion())
                                    .scheme(jvbInstance.getScheme())
                                    .host(jvbInstance.getHost())
                                    .port(jvbInstance.getPort())
                                    .forSpeakers(jvbInstance.isForSpeakers())
                                    .octoBindPort(jvbInstance.getOctoBindPort())
                                    .needShutdown(needShutdown(jvbInstance))
                                    .responding(true)
                                    .respondedOnce(true)
                                    .scheduledForRemoval(stats.isShutdownInProgress() || prevJvbInstanceDataOptional
                                            .map(JvbInstanceData::isScheduledForRemoval)
                                            .orElse(false)
                                    )
                                    .shutdownInProgress(stats.isShutdownInProgress())
                                    .utilization(100 * (Math.max(stats.getEndpointsSendingVideo(), stats.getEndpointsSendingAudio()) + stats.getReceiveOnlyEndpoints()/2) / jvbInstanceEndpointsCapacity);
                            if (jvbInstance.isNodeExporterAvailable()) {
                                try {
                                    Map<String, String> metrics = nodeExporterService.getMetrics(jvbInstance.getHost(), List.of("node_time_seconds", "node_cpu_seconds_total\\{cpu=\"[a-zA-Z0-9]+\",mode=\"idle\"\\}"));
                                    Double nodeTime = Double.parseDouble(metrics.get("node_time_seconds"));
                                    List<Double> nodeCpuIdle = metrics.entrySet().stream()
                                            .filter(x -> x.getKey().startsWith("node_cpu_seconds_total"))
                                            .map(Map.Entry::getValue)
                                            .map(Double::parseDouble)
                                            .collect(Collectors.toList());
                                    jvbInstanceDataBuilder
                                            .nodeTime(nodeTime)
                                            .nodeCpuIdleTotal(nodeCpuIdle.stream().mapToDouble(x -> x).sum())
                                            .nodeCpuCount(nodeCpuIdle.size());
                                    prevJvbInstanceDataOptional.ifPresent(prevJvbInstanceData -> {
                                        if (prevJvbInstanceData.getNodeTime() != null
                                                && prevJvbInstanceData.getNodeTime() < nodeTime
                                                && prevJvbInstanceData.getNodeCpuCount() == nodeCpuIdle.size()
                                        ) {
                                            double cpuIdleTotal = nodeCpuIdle.stream().mapToDouble(x -> x).sum();
                                            double timeInterval = (nodeTime - prevJvbInstanceData.getNodeTime()) * nodeCpuIdle.size();
                                            jvbInstanceDataBuilder.cpuLoad(
                                                    1 - (cpuIdleTotal - prevJvbInstanceData.getNodeCpuIdleTotal()) / timeInterval
                                            );
                                        }
                                    });
                                } catch (Exception e) {
                                    log.warn("Node exporter metrics error", e);
                                }
                            }
                            jvbInstanceDataList.add(jvbInstanceDataBuilder.build());
                        }
                    } catch (Exception e) {
                        log.error("Processing jvbInstance(id={}) failed", jvbInstance.getId(), e);
                    } finally {
                        mdcCopyHelper.clear();
                    }
                });

        if(!instancesIdsForClearConferences.isEmpty()) {
            jvbConferenceDataRepository.deleteAllByInstanceIdIn(instancesIdsForClearConferences);
        }

        List<String> ids = jvbInstanceDataList.stream()
                .map(JvbInstanceData::getId)
                .collect(Collectors.toList());
        List<JvbInstanceData> instancesToDelete;
        if(ids.isEmpty()) {
            instancesToDelete = jvbInstanceDataRepository.findAll();
        } else {
            instancesToDelete = jvbInstanceDataRepository.findAllByIdIsNotIn(ids);
        }
        for(JvbInstanceData instanceData : instancesToDelete) {
            log.info("Deleting not detected instance(id={})", instanceData.getId());
            logInstanceConferences(instanceData);
        }
        jvbInstanceDataRepository.deleteAll(instancesToDelete);
        for(JvbInstanceData instanceData : jvbInstanceDataList) {
            if(jvbInstanceDataRepository.existsById(instanceData.getId())) continue;
            log.info("Saving detected new instance(id={}, isForSpeakers={})", instanceData.getId(), instanceData.isForSpeakers());
        }
        jvbInstanceDataRepository.saveAll(jvbInstanceDataList);
    }

    @Override
    @Transactional
    public void stopNotRespondingTooLongInstances() {
        List<JvbInstanceData> respondedOnceDeadInstances = jvbInstanceDataRepository.findAllByNotRespondingSinceIsBeforeAndRespondedOnce(LocalDateTime.now().minusSeconds(5), true);
        if (respondedOnceDeadInstances.size() > 0) {
            log.warn("Too long inactive(5 seconds) instances(respondedOnce=true) - {}", respondedOnceDeadInstances);
        }
        List<JvbInstanceData> notRespondedOnceDeadInstances = jvbInstanceDataRepository.findAllByNotRespondingSinceIsBeforeAndRespondedOnce(LocalDateTime.now().minusMinutes(5), false);
        if (notRespondedOnceDeadInstances.size() > 0) {
            log.warn("Too long inactive(5 minutes) instances(respondedOnce=false) - {}", notRespondedOnceDeadInstances);
        }
        MDCCopyHelper mdcCopyHelper = new MDCCopyHelper();
        List<JvbInstanceData> stoppedInstances = Stream.concat(respondedOnceDeadInstances.stream(), notRespondedOnceDeadInstances.stream()).parallel()
                .peek(mdcCopyHelper::set)
                .filter(mdcCopyHelper.clearIfFalse(x -> jvbInstanceManagementService.stop(x.getId())))
                .peek(mdcCopyHelper::clear)
                .collect(Collectors.toList());
        for(JvbInstanceData instanceData : stoppedInstances) {
            log.warn("Deleting stopped instance(id={})", instanceData.getId());
            logInstanceConferences(instanceData);
        }
        jvbInstanceDataRepository.deleteAll(stoppedInstances);
    }

    private static void logInstanceConferences(JvbInstanceData instanceData) {
        if(instanceData.getConferences().size() > 0) {
            String conferencesToDelete = instanceData.getConferences().stream()
                    .map(JvbConferenceData::getId)
                    .collect(Collectors.joining(", "));
            String endpointsToDelete = instanceData.getConferences().stream()
                    .flatMap(x->x.getEndpoints().stream())
                    .map(JvbEndpointData::getId)
                    .distinct()
                    .collect(Collectors.joining(", "));
            log.warn("Deleting an instance(id={}) will cause conferences([{}]) and endpoints([{}]) to be deleted",
                    instanceData.getId(),
                    conferencesToDelete,
                    endpointsToDelete
            );
        }
    }

    @Override
    @Transactional
    public void scaleInstances(boolean forSpeakers) {
        List<JvbInstanceData> allJvbInstances = jvbInstanceDataRepository.findAllByForSpeakers(forSpeakers);
        if (!allJvbInstances.stream().allMatch(JvbInstanceData::isResponding)) {
            return;
        }

        List<JvbInstanceData> normalJvbInstances = allJvbInstances.stream()
                .filter(x -> !x.isScheduledForRemoval() && !x.isNeedShutdown())
                .collect(Collectors.toList());

        if (normalJvbInstances.size() < jvbMinPoolSize) {
            log.info("The size of running normal JVB instances(forSpeakers={}) is less than minimum pool size.", forSpeakers);
            List<String> startedInstances = new ArrayList<>();
            for (int i = normalJvbInstances.size(); i < jvbMinPoolSize; i++) {
                startedInstances.add(jvbInstanceManagementService.start(forSpeakers));
            }
            log.info("New instances({}, forSpeakers={}) have been started", startedInstances, forSpeakers);
        } else if (normalJvbInstances.stream().allMatch(x -> x.getUtilization() > jvbInstanceMaxUtilization)) {
            log.info("All normal JVB instances(forSpeakers={}) utilized more than {} percents", forSpeakers, jvbInstanceMaxUtilization);
            List<JvbInstanceData> scheduledForRemovalJvbInstances = allJvbInstances.stream()
                    .filter(x -> x.isScheduledForRemoval() && !x.isShutdownInProgress() && !x.isNeedShutdown())
                    .collect(Collectors.toList());
            if (scheduledForRemovalJvbInstances.size() > 0) {
                for (JvbInstanceData jvbInstanceData : scheduledForRemovalJvbInstances) {
                    jvbInstanceData.setScheduledForRemoval(false);
                    jvbInstanceDataRepository.save(jvbInstanceData);
                    log.info("JVB instance(id={}, forSpeakers={}) is removed from the ones scheduled for removal", jvbInstanceData.getId(), forSpeakers);
                }
            } else {
                jvbInstanceManagementService.start(forSpeakers);
                log.info("New JVB instance(forSpeakers={}) has been started", forSpeakers);
            }
        } else if (
                allJvbInstances.stream().noneMatch(JvbInstanceData::isScheduledForRemoval) && allJvbInstances.stream().noneMatch(JvbInstanceData::isNeedShutdown)
                        && allJvbInstances.size() > jvbMinPoolSize && allJvbInstances.stream().mapToInt(JvbInstanceData::getUtilization).allMatch(x -> x < jvbInstanceMinUtilization)
        ) {
            log.info("JVB instances(forSpeakers={}) are too lightly loaded", forSpeakers);
            allJvbInstances.stream()
                    .min(Comparator.comparing(JvbInstanceData::getUtilization))
                    .ifPresent(jvb -> {
                        jvb.setScheduledForRemoval(true);
                        jvbInstanceDataRepository.save(jvb);
                        log.info("JVB instance(id={}, forSpeakers={}) has been scheduled for removal", jvb.getId(), forSpeakers);
                    });
        } else if (normalJvbInstances.size() >= jvbMinPoolSize) {
            allJvbInstances.stream()
                    .filter(x -> x.isNeedShutdown() && !x.isScheduledForRemoval())
                    .findFirst()
                    .ifPresent(jvbInstanceData -> {
                        jvbInstanceData.setScheduledForRemoval(true);
                        jvbInstanceDataRepository.save(jvbInstanceData);
                        log.info("JVB instance(id={}, forSpeakers={}) has been scheduled for removal", jvbInstanceData.getId(), forSpeakers);
                    });
        }
        jvbInstanceDataRepository.findAllByScheduledForRemovalIsTrueAndShutdownInProgressIsFalseAndForSpeakers(forSpeakers)
                .forEach(jvbInstance -> {
                    if (jvbConferenceDataRepository.countByInstanceId(jvbInstance.getId()) == 0) {
                        try {
                            JvbInstance.from(jvbInstance, okHttpClient).shutdown(true);
                            jvbInstance.setShutdownInProgress(true);
                            jvbInstanceDataRepository.save(jvbInstance);
                            log.info("JVB instance(id={}, forSpeakers={}) has been shutdown because it has no conferences and was scheduled for removal", jvbInstance.getId(), forSpeakers);
                        } catch (Exception e) {
                            log.error("Failed to shutdown JvbInstance(id={}, forSpeakers={})", jvbInstance.getId(), forSpeakers, e);
                        }
                    }
                });
    }
}
