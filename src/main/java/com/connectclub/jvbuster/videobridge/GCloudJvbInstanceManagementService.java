package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.utils.gcloud.*;
import com.connectclub.jvbuster.videobridge.i.JvbInstanceManagementService;
import com.google.cloud.compute.v1.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@Profile("gcloud-jvb")
public class GCloudJvbInstanceManagementService implements JvbInstanceManagementService, DisposableBean {

    private final static Set<Instance.Status> activeStatuses = Set.of(
            Instance.Status.PROVISIONING,
            Instance.Status.STAGING,
            Instance.Status.RUNNING
    );

    private final InstancesClient instanceClient;
    private final DisksClient diskClient;
    private final String project;
    private final String zone;
    private final String region;
    private final String appLabel;
    private final String diskSourceImageProject;
    private final String diskSourceImage;
    private final String subnet;
    private final String conferenceNotificationUrl;
    private final String statisticNotificationUrl;
    private final String audioProcessorHttpUrl;
    private final String audioProcessorIp;
    private final String machineType;
    private final boolean usePublicIpForRest;

    private final OkHttpClient okHttpClient;

    public GCloudJvbInstanceManagementService(
            @Value("${gcloud.jvb.project}") String project,
            @Value("${gcloud.jvb.zone}") String zone,
            @Value("${gcloud.jvb.app-label}") String appLabel,
            @Value("${gcloud.jvb.disk-source-image.project}") String diskSourceImageProject,
            @Value("${gcloud.jvb.disk-source-image}") String diskSourceImage,
            @Value("${gcloud.jvb.subnet}") String subnet,
            @Value("${jvb.conference.notification.url}") String conferenceNotificationUrl,
            @Value("${jvb.statistic.notification.url}") String statisticNotificationUrl,
            @Value("${jvb.audio.processor.http.url}") String audioProcessorHttpUrl,
            @Value("${jvb.audio.processor.ip}") String audioProcessorIp,
            @Value("${jvb.machine.type}") String machineType,
            @Value("${jvb.use-public-ip-for-rest}") boolean usePublicIpForRest,
            OkHttpClient okHttpClient
    ) throws IOException {
        this.project = project;
        this.zone = zone;
        this.region = zone.substring(0, zone.lastIndexOf('-'));
        this.appLabel = appLabel;
        this.diskSourceImageProject = diskSourceImageProject;
        this.diskSourceImage = diskSourceImage;
        this.subnet = subnet;
        this.conferenceNotificationUrl = conferenceNotificationUrl;
        this.statisticNotificationUrl = statisticNotificationUrl;
        this.audioProcessorHttpUrl = audioProcessorHttpUrl;
        this.audioProcessorIp = audioProcessorIp;
        this.machineType = machineType;
        this.usePublicIpForRest = usePublicIpForRest;

        instanceClient = InstancesClient.create(
//                InstancesSettings.newBuilder(
//                        ClientContext.create(InstancesStubSettings.newBuilder().build()).toBuilder()
//                                .setDefaultCallContext(HttpJsonCallContext.createDefault().withTimeout(org.threeten.bp.Duration.ofMinutes(1)))
//                                .build()
//                ).build()
        );
        diskClient = DisksClient.create(
//                DisksSettings.newBuilder(
//                        ClientContext.create(InstancesStubSettings.newBuilder().build()).toBuilder()
//                                .setDefaultCallContext(HttpJsonCallContext.createDefault().withTimeout(org.threeten.bp.Duration.ofMinutes(1)))
//                                .build()
//                ).build()
        );

        this.okHttpClient = okHttpClient;
    }

    @Override
    public String start(boolean forSpeakers) {
        Tags.Builder networkTags = Tags.newBuilder()
                .addItems("videobridge");
        if (usePublicIpForRest) {
            networkTags.addItems("videobridge-public");
        }
        String instanceName = "videobridge-" + UUID.randomUUID();
        InsertInstanceRequest insertInstanceHttpRequest = InsertInstanceRequest.newBuilder()
                .setProject(project)
                .setZone(zone)
                .setInstanceResource(Instance.newBuilder()
                        .setName(instanceName)
                        .addNetworkInterfaces(NetworkInterface.newBuilder()
                                .addAccessConfigs(AccessConfig.newBuilder()
                                        .setName("external-nat")
                                        .setType(AccessConfig.Type.ONE_TO_ONE_NAT)
                                        .build())
                                .setSubnetwork(ProjectRegionSubnetworkName.format(project, region, subnet))
                                .build())
                        .setZone(zone)
                        .setTags(networkTags.build())
                        .setMachineType(ProjectZoneMachineTypeName.format(machineType, project, zone))
                        .addDisks(AttachedDisk.newBuilder()
                                .setAutoDelete(true)
                                .setBoot(true)
                                .setInitializeParams(AttachedDiskInitializeParams.newBuilder()
                                        .setDiskSizeGb(50)
                                        .setDiskType(ProjectZoneDiskTypeName.format("pd-standard", project, zone))
                                        .setSourceImage(ProjectGlobalImageName.format(diskSourceImage, diskSourceImageProject))
                                        .build())
                                .build())
                        .putAllLabels(Map.of("app", appLabel))
                        .setMetadata(
                                Metadata.newBuilder()
                                        .addItems(Items.newBuilder()
                                                .setKey("JVB_CONFERENCE_NOTIFICATION_URL")
                                                .setValue(conferenceNotificationUrl)
                                                .build())
                                        .addItems(Items.newBuilder()
                                                .setKey("JVB_STATISTIC_NOTIFICATION_URL")
                                                .setValue(statisticNotificationUrl)
                                                .build())
                                        .addItems(Items.newBuilder()
                                                .setKey("FOR_SPEAKERS")
                                                .setValue(String.valueOf(forSpeakers))
                                                .build())
                                        .addItems(Items.newBuilder()
                                                .setKey("AUDIO_PROCESSOR_HTTP_URL")
                                                .setValue(audioProcessorHttpUrl)
                                                .build())
                                        .addItems(Items.newBuilder()
                                                .setKey("AUDIO_PROCESSOR_IP")
                                                .setValue(audioProcessorIp)
                                                .build())
                                        .build()
                        )
                        .build())
                .build();
        instanceClient.insert(insertInstanceHttpRequest);
        return instanceName;
    }

    @Override
    public boolean stop(String instanceId) {
        try {
            Operation operation = instanceClient.delete(project, zone, instanceId);
            log.info("deleteInstance operation=\n{}", operation);
            return true;
        } catch (Exception e) {
            log.error("Can not destroy instance(id={})", instanceId, e);
            return false;
        }
    }

    @Override
    public List<JvbInstance> getActive() {
        ListInstancesRequest listInstancesHttpRequest = ListInstancesRequest.newBuilder()
                .setProject(project)
                .setZone(zone)
                .setMaxResults(Integer.MAX_VALUE)
                .setFilter("labels.app=" + appLabel)
                .build();
        InstancesClient.ListPagedResponse listInstancesPagedResponse = instanceClient.list(listInstancesHttpRequest);
        return StreamSupport.stream(listInstancesPagedResponse.iterateAll().spliterator(), false)
                .filter(x -> activeStatuses.contains(x.getStatus()))
                .map(GCloudJvbInstanceInfo::new)
                .map(x -> new JvbInstance(x.name, x.diskSourceImage, "http", x.ip, 8080, x.creationTimestamp, true, x.forSpeakers, x.octoBindPort, okHttpClient))
                .collect(Collectors.toList());
    }

    @Override
    public String getLastVersion() {
        return diskSourceImage;
    }

    @Override
    public void destroy() {
        instanceClient.close();
    }

    private class GCloudJvbInstanceInfo {
        private final String name;
        private final String ip;
        private final String diskSourceImage;
        private final Instant creationTimestamp;
        private final boolean forSpeakers;
        private final Integer octoBindPort;

        public GCloudJvbInstanceInfo(Instance instance) {
            name = instance.getName();
            if (usePublicIpForRest) {
                ip = instance.getNetworkInterfacesList().stream()
                        .map(NetworkInterface::getAccessConfigsList)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .map(AccessConfig::getNatIP)
                        .filter(Objects::nonNull)
                        .findFirst().orElse("public-ip-not-available");
            } else {
                ip = instance.getNetworkInterfacesList().stream()
                        .map(NetworkInterface::getNetworkIP)
                        .filter(Objects::nonNull)
                        .findFirst().orElse("private-ip-not-available");
            }

            ProjectZoneDiskName projectZoneDiskName = ProjectZoneDiskName.parse(instance.getDisksList().get(0).getSource());
            Disk disk = diskClient.get(projectZoneDiskName.getProject(), projectZoneDiskName.getZone(), projectZoneDiskName.getDisk());
            diskSourceImage = ProjectGlobalImageName.parse(disk.getSourceImage()).getImage();
            creationTimestamp = ZonedDateTime.parse(instance.getCreationTimestamp()).toInstant();
            forSpeakers = instance.getMetadata().getItemsList().stream()
                    .filter(x -> "FOR_SPEAKERS".equals(x.getKey()))
                    .map(Items::getValue)
                    .map(Boolean::parseBoolean)
                    .findFirst().orElse(false);
            octoBindPort = 4096;
        }
    }
}
