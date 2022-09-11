package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.videobridge.i.JvbInstanceManagementService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@Profile("native-jvb")
public class NativeJvbInstanceManagementService implements JvbInstanceManagementService, DisposableBean {

    private static final String jvbConfContent = "videobridge { octo { enabled = %s, bind-port = %s, bind-address = 0.0.0.0}, ice { udp { port = %s } }}";
    private static final Pattern jvbConfContentPattern = Pattern.compile(
            "^videobridge\\s*\\{\\s*octo\\s*\\{\\s*enabled\\s*=\\s*(?<octoEnabled>\\w+)\\s*,\\s*bind-port\\s*=\\s*(?<octoBindPort>\\d+)\\s*,\\s*bind-address\\s*=\\s*0\\.0\\.0\\.0\\s*}\\s*,\\s*ice\\s*\\{\\s*udp\\s*\\{\\s*port\\s*=\\s*(?<iceUdpPort>\\d+)\\s*}\\s*}\\s*}$"
    );

    private final String workDir;
    private final String javaPath;
    private final String conferenceNotificationUrl;
    private final String statisticNotificationUrl;
    private final String audioProcessorHttpUrl;
    private final String audioProcessorIp;
    private final List<Process> processes = new ArrayList<>();

    private final OkHttpClient okHttpClient;

    public NativeJvbInstanceManagementService(
            @Value("${native.jvb.work.dir}") String workDir,
            @Value("${native.jvb.java.path}") String javaPath,
            @Value("${jvb.conference.notification.url}") String conferenceNotificationUrl,
            @Value("${jvb.statistic.notification.url}") String statisticNotificationUrl,
            @Value("${jvb.audio.processor.http.url}") String audioProcessorHttpUrl,
            @Value("${jvb.audio.processor.ip}") String audioProcessorIp,
            OkHttpClient okHttpClient
    ) {
        this.workDir = workDir;
        this.javaPath = javaPath;
        this.conferenceNotificationUrl = conferenceNotificationUrl;
        this.statisticNotificationUrl = statisticNotificationUrl;
        this.audioProcessorHttpUrl = audioProcessorHttpUrl;
        this.audioProcessorIp = audioProcessorIp;
        this.okHttpClient = okHttpClient;
    }

    @Override
    @SneakyThrows
    public String start(boolean forSpeakers) {
        int jettyPort;
        int debuggerPort;
        Path jvbConfPath = Files.createTempFile("jvb-application-", ".conf");
        try (
                ServerSocket jettySocket = new ServerSocket(0);
                ServerSocket iceUdpSocket = new ServerSocket(0);
                ServerSocket octoBindSocket = new ServerSocket(0);
                ServerSocket debuggerSocket = new ServerSocket(0);
        ) {
            jettyPort = jettySocket.getLocalPort();
            debuggerPort = debuggerSocket.getLocalPort();
            Files.writeString(jvbConfPath, String.format(jvbConfContent, true, octoBindSocket.getLocalPort(), iceUdpSocket.getLocalPort()));
        }

        String[] cmdarray = new String[]{
                javaPath,
                "-Dorg.jitsi.videobridge.rest.private.jetty.port=" + jettyPort,
                "-Dconfig.file=" + jvbConfPath,
                "-Dconference.notification.url=" + conferenceNotificationUrl,
                "-Dstatistic.notification.url=" + statisticNotificationUrl,
                "-Daudio.processor.http.url=" + audioProcessorHttpUrl,
                "-Daudio.processor.ip=" + audioProcessorIp,
                "-Dorg.jitsi.videobridge.ENABLE_REST_SHUTDOWN=true",
                "-Dorg.jitsi.videobridge.shutdown.ALLOWED_SOURCE_REGEXP=.*",
                "-Djmt.debug.pcap.enabled=true",
                "-DforSpeakers=" + forSpeakers,
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + debuggerPort,
                "-cp",
                "./jitsi-videobridge.jar:./lib/*",
                "org.jitsi.videobridge.Main",
                "--apis=rest"
        };
        Path logPath = Files.createTempFile("jvb-instance-", "-" + jettyPort + ".log");
        Process process = new ProcessBuilder(cmdarray)
                .directory(Path.of(workDir).toFile())
                .redirectError(logPath.toFile())
                .redirectOutput(logPath.toFile())
                .start();
        log.info("JVB instance launched(pid={}, forSpeakers={}, debuggerPort={}, logPath={})", process.pid(), forSpeakers, debuggerPort, logPath);
        processes.add(process);
        return String.valueOf(process.pid());
    }

    @Override
    public boolean stop(String instanceId) {
        try {
            return ProcessHandle.of(Long.parseLong(instanceId)).get().destroy();
        } catch (Exception e) {
            log.error("Can not destroy instance(id={})", instanceId, e);
            return false;
        }
    }

    @Override
    public List<JvbInstance> getActive() {
        List<ProcessHandle> processHandles = ProcessHandle.allProcesses().collect(Collectors.toList());
        return processHandles.stream()
                .filter(x -> Objects.equals(x.info().command().orElse(""), javaPath))
                .filter(x -> Set.of(x.info().arguments().orElse(new String[0])).contains("org.jitsi.videobridge.Main"))
                .map(NativeJvbInstanceInfo::new)
                .map(x -> new JvbInstance(String.valueOf(x.pid), "no-version", "http", "localhost", x.httpPort, x.startInstant, false, x.forSpeakers, x.octoBindPort, okHttpClient))
                .collect(Collectors.toList());
    }

    @Override
    public String getLastVersion() {
        return "no-version";
    }

    @Override
    public void destroy() {
        processes.forEach(Process::destroy);
    }

    private static class NativeJvbInstanceInfo {
        private final long pid;
        private final Instant startInstant;
        private final int httpPort;
        private final boolean forSpeakers;
        private final int octoBindPort;

        @SneakyThrows
        public NativeJvbInstanceInfo(ProcessHandle ph) {
            pid = ph.pid();
            startInstant = ph.info().startInstant().orElse(null);
            httpPort = Arrays.stream(ph.info().arguments().get())
                    .filter(x -> x.startsWith("-Dorg.jitsi.videobridge.rest.private.jetty.port="))
                    .map(x -> x.split("=")[1])
                    .map(Integer::parseInt)
                    .findFirst().orElseThrow();
            forSpeakers = Arrays.stream(ph.info().arguments().get())
                    .filter(x -> x.startsWith("-DforSpeakers="))
                    .map(x -> x.split("=")[1])
                    .map(Boolean::parseBoolean)
                    .findFirst().orElse(false);
            String jvbConfPath = Arrays.stream(ph.info().arguments().get())
                    .filter(x -> x.startsWith("-Dconfig.file="))
                    .map(x -> x.split("=")[1])
                    .findFirst().orElseThrow();
            Matcher matcher = jvbConfContentPattern.matcher(Files.readString(Paths.get(jvbConfPath)));
            if(!matcher.matches()) {
                throw new RuntimeException("!matcher.matches()");
            }
            octoBindPort = Integer.parseInt(matcher.group("octoBindPort"));
        }
    }

}
