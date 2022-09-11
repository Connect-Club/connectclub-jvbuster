package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.videobridge.i.JvbInstanceManagementService;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Profile("docker-jvb")
public class DockerJvbInstanceManagementService implements JvbInstanceManagementService, DisposableBean {

    private final String conferenceNotificationUrl;
    private final String statisticNotificationUrl;
    private final String audioProcessorHttpUrl;
    private final String audioProcessorIp;
    private final String dockerHostAddress;
    private final String dockerImage;

    private final List<Process> processes = new ArrayList<>();

    private final OkHttpClient okHttpClient;

    private final String jvbNetwork;

    public DockerJvbInstanceManagementService(
            @Value("${jvb.conference.notification.url}") String conferenceNotificationUrl,
            @Value("${jvb.statistic.notification.url}") String statisticNotificationUrl,
            @Value("${jvb.audio.processor.http.url}") String audioProcessorHttpUrl,
            @Value("${jvb.audio.processor.ip}") String audioProcessorIp,
            @Value("${jvb.docker-host-address}") String dockerHostAddress,
            @Value("${jvb.docker-image}") String dockerImage,
            OkHttpClient okHttpClient
    ) throws IOException {
        this.conferenceNotificationUrl = conferenceNotificationUrl;
        this.statisticNotificationUrl = statisticNotificationUrl;
        this.audioProcessorHttpUrl = audioProcessorHttpUrl;
        this.audioProcessorIp = audioProcessorIp;
        this.dockerHostAddress = dockerHostAddress;
        this.dockerImage = dockerImage;
        this.okHttpClient = okHttpClient;

        if (Files.exists(Path.of("/", ".dockerenv"))) {
            Process networkProcess = new ProcessBuilder(
                    "docker",
                    "inspect",
                    InetAddress.getLocalHost().getHostName(),
                    "--format",
                    "{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}"
            ).start();
            jvbNetwork = CharStreams.toString(new InputStreamReader(networkProcess.getInputStream())).split(" ")[0];
        } else {
            jvbNetwork = "bridge";
        }
        log.info("jvbNetwork={}", jvbNetwork);
    }

    @Override
    @SneakyThrows
    public String start(boolean forSpeakers) {
        int iceUdpPort;
        try (
                ServerSocket iceUdpSocket = new ServerSocket(0);
        ) {
            iceUdpPort = iceUdpSocket.getLocalPort();
        }

        String jvbId = UUID.randomUUID().toString();
        String containerName = "jvb-" + jvbId;
        String[] cmdarray = new String[]{
                "docker",
                "run",
                "--rm",
                "--name", containerName,
                "--label", "forSpeakers=" + forSpeakers,
                "--network", jvbNetwork,
                "-p", iceUdpPort + ":" + iceUdpPort + "/udp",
                "-e", "DOCKER_HOST_ADDRESS=" + dockerHostAddress,
                "-e", "JAVA_SYS_PROPS",
                dockerImage,
        };
        Path logPath = Files.createTempFile("jvb-instance-", "-" + jvbId + ".log");
        ProcessBuilder processBuilder = new ProcessBuilder(cmdarray)
                .redirectError(logPath.toFile())
                .redirectOutput(logPath.toFile());
        processBuilder.environment().put("JAVA_SYS_PROPS", String.format("-Dorg.ice4j.ice.harvest.STUN_MAPPING_HARVESTER_ADDRESSES=meet-jit-si-turnrelay.jitsi.net:443 " +
                        "-Dorg.ice4j.ipv6.DISABLED=true " +
                        "-Dorg.jitsi.videobridge.ENABLE_REST_SHUTDOWN=true " +
                        "-Dorg.jitsi.videobridge.shutdown.ALLOWED_SOURCE_REGEXP=.* " +
                        "-Dconference.notification.url=%s " +
                        "-Dstatistic.notification.url=%s " +
                        "-Daudio.processor.http.url=%s " +
                        "-Daudio.processor.ip=%s " +
                        "-Dvideobridge.ice.udp.port=%s",
                conferenceNotificationUrl,
                statisticNotificationUrl,
                audioProcessorHttpUrl,
                audioProcessorIp,
                iceUdpPort));
        Process process = processBuilder.start();
        log.info("JVB instance launched(pid={}, forSpeakers={}, logPath={})", process.pid(), forSpeakers, logPath);
        processes.add(process);
        return containerName;
    }

    @Override
    public boolean stop(String instanceId) {
        try {
            Process p = new ProcessBuilder("docker", "stop", instanceId).start();
            p.waitFor();
            return true;
        } catch (Exception e) {
            log.error("Can not destroy instance(id={})", instanceId, e);
            return false;
        }
    }

    @Override
    @SneakyThrows
    public List<JvbInstance> getActive() {
        Process p = new ProcessBuilder("bash", "-c",
                "docker ps --filter name=jvb- --format '{{.ID}}' | xargs docker inspect --format '{{.Name}} {{.Created}} {{.Config.Labels.forSpeakers}} {{range $k,$v := .NetworkSettings.Networks}}{{$k}}:{{$v.IPAddress}} {{end}}'"
        ).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return reader.lines().map(DockerJvbInstanceInfo::new)
                .map(x -> new JvbInstance(
                        x.containerName,
                        "no-version",
                        "http",
                        x.ip,
                        8080,
                        x.startInstant,
                        false,
                        x.forSpeakers,
                        4096,
                        okHttpClient
                )).collect(Collectors.toList());
    }

    @Override
    public String getLastVersion() {
        return "no-version";
    }

    @Override
    public void destroy() {
        processes.forEach(Process::destroy);
    }

    private class DockerJvbInstanceInfo {
        private final String containerName;
        private final Instant startInstant;
        private final boolean forSpeakers;
        private final String ip;

        public DockerJvbInstanceInfo(String containerInfoLine) {
            String[] info = containerInfoLine.split(" ");
            containerName = info[0].substring(1);
            startInstant = Instant.parse(info[1]);
            forSpeakers = Boolean.parseBoolean(info[2]);

            String ip = "";
            for (int i = 3; i < info.length; i++) {
                String[] network = info[i].split(":");
                if (Objects.equals(network[0], jvbNetwork)) {
                    ip = network[1];
                    break;
                }
            }
            this.ip = ip;
        }
    }
}
