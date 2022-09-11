package com.connectclub.jvbuster.monitoring;

import com.connectclub.jvbuster.monitoring.i.JvbInstancesTasksService;
import com.connectclub.jvbuster.nodeexporter.i.NodeExporterService;
import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import com.connectclub.jvbuster.repository.i.JvbConferenceDataRepository;
import com.connectclub.jvbuster.repository.i.JvbInstanceDataRepository;
import com.connectclub.jvbuster.videobridge.JvbInstance;
import com.connectclub.jvbuster.videobridge.data.jvb.Stats;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.connectclub.jvbuster.videobridge.i.JvbInstanceManagementService;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultJvbInstancesTasksServiceTest {

    private final static int JVB_MIN_POOL_SIZE = 3;

    @Mock
    private JvbInstanceDataRepository jvbInstanceDataRepository;

    @Mock
    private JvbConferenceDataRepository jvbConferenceDataRepository;

    @Mock
    private JvbInstanceManagementService jvbInstanceManagementService;

    @Mock
    private NodeExporterService nodeExporterService;

    private JvbInstancesTasksService jvbInstancesTasksService;

    @BeforeEach
    public void beforeClass() {
        jvbInstancesTasksService = new DefaultJvbInstancesTasksService(
                JVB_MIN_POOL_SIZE,
                10,
                80,
                50,
                1,
                jvbInstanceManagementService,
                jvbInstanceDataRepository,
                jvbConferenceDataRepository,
                nodeExporterService,
                null
        );
    }

    @Test
    public void cacheInstances_cacheValidationException() {
        when(jvbInstanceDataRepository.findAll()).thenReturn(Collections.singletonList(null));

        assertThrows(RuntimeException.class, () -> jvbInstancesTasksService.cacheInstances());
    }

    @Test
    public void cacheInstancesTest() throws IOException, JvbInstanceRestException {
        JvbInstance jvbInstance1 = mock(JvbInstance.class);
        when(jvbInstance1.getId()).thenReturn("JvbInstance1");
        when(jvbInstance1.getVersion()).thenReturn("v0");
        when(jvbInstance1.getScheme()).thenReturn("http");
        when(jvbInstance1.getHost()).thenReturn("host1");
        when(jvbInstance1.getPort()).thenReturn(8001);
        when(jvbInstance1.isNodeExporterAvailable()).thenReturn(true);
        when(jvbInstance1.getStats()).thenReturn(
                Stats.builder()
                        .endpointsSendingAudio(1)
                        .endpointsSendingVideo(1)
                        .shutdownInProgress(false)
                        .build()
        );

        JvbInstance jvbInstance2 = mock(JvbInstance.class);
        when(jvbInstance2.getId()).thenReturn("JvbInstance2");
        when(jvbInstance2.getVersion()).thenReturn("v1");
        when(jvbInstance2.getScheme()).thenReturn("http");
        when(jvbInstance2.getHost()).thenReturn("host2");
        when(jvbInstance2.getPort()).thenReturn(8002);
        when(jvbInstance2.getStats()).thenThrow(RuntimeException.class);

        JvbInstance jvbInstance3 = mock(JvbInstance.class);
        when(jvbInstance3.getId()).thenReturn("JvbInstance3");
        when(jvbInstance3.getVersion()).thenReturn("v1");
        when(jvbInstance3.getScheme()).thenReturn("http");
        when(jvbInstance3.getHost()).thenReturn("host3");
        when(jvbInstance3.getPort()).thenReturn(8003);
        when(jvbInstance3.isNodeExporterAvailable()).thenReturn(true);
        when(jvbInstance3.getStats()).thenReturn(
                Stats.builder()
                        .endpointsSendingAudio(1)
                        .endpointsSendingVideo(1)
                        .shutdownInProgress(false)
                        .build()
        );

        JvbInstance jvbInstance4 = mock(JvbInstance.class);
        when(jvbInstance4.getId()).thenReturn("JvbInstance4");
        when(jvbInstance4.getVersion()).thenReturn("v0");
        when(jvbInstance4.getScheme()).thenReturn("http");
        when(jvbInstance4.getHost()).thenReturn("host4");
        when(jvbInstance4.getPort()).thenReturn(8004);
        when(jvbInstance4.isNodeExporterAvailable()).thenReturn(false);
        when(jvbInstance4.getStats()).thenReturn(
                Stats.builder()
                        .endpointsSendingAudio(1)
                        .endpointsSendingVideo(1)
                        .shutdownInProgress(false)
                        .build()
        );
        doThrow(RuntimeException.class).when(jvbInstance4).shutdown(anyBoolean());

        JvbInstanceData jvbInstanceData1 = JvbInstanceData.builder()
                .id("JvbInstance2")
                .version("v0")
                .scheme("http")
                .host("host2")
                .port(8001)
                .respondedOnce(true)
                .scheduledForRemoval(false)
                .shutdownInProgress(false)
                .utilization(null)
                .nodeTime(0.0)
                .nodeCpuCount(2)
                .nodeCpuIdleTotal(0.0)
                .build();

        JvbInstanceData jvbInstanceData2 = JvbInstanceData.builder()
                .id("JvbInstance2")
                .version("v1")
                .scheme("http")
                .host("host2")
                .port(8002)
                .respondedOnce(false)
                .scheduledForRemoval(false)
                .shutdownInProgress(false)
                .utilization(null)
                .notRespondingSince(LocalDateTime.MIN)
                .build();

        when(nodeExporterService.getMetrics(eq("host1"), anyCollection())).thenReturn(Map.of(
                "node_time", "100.0",
                "node_cpu{cpu=\"0\",mode=\"idle\"}", "60.0",
                "node_cpu{cpu=\"1\",mode=\"idle\"}", "40.0"
        ));
        when(nodeExporterService.getMetrics(eq("host3"), anyCollection())).thenThrow(RuntimeException.class);

        when(jvbInstanceDataRepository.findAll()).thenReturn(List.of(jvbInstanceData1, jvbInstanceData2));
        when(jvbInstanceDataRepository.findById("JvbInstance1")).thenReturn(Optional.of(jvbInstanceData1));
        when(jvbInstanceDataRepository.findById("JvbInstance2")).thenReturn(Optional.of(jvbInstanceData2));
        when(jvbInstanceDataRepository.findById("JvbInstance3")).thenReturn(Optional.empty());
        when(jvbInstanceManagementService.getActive()).thenReturn(List.of(jvbInstance1, jvbInstance2, jvbInstance3, jvbInstance4));
        when(jvbInstanceManagementService.getLastVersion()).thenReturn("v1");

        jvbInstancesTasksService.cacheInstances();

        verify(jvbInstanceDataRepository).deleteAll(List.of());
        verify(jvbInstanceDataRepository).saveAll(argThat(arg -> Sets.newHashSet(arg).equals(Set.of(
                JvbInstanceData.builder()
                        .id("JvbInstance1")
                        .version("v0")
                        .scheme("http")
                        .host("host1")
                        .port(8001)
                        .needShutdown(true)
                        .responding(true)
                        .respondedOnce(true)
                        .scheduledForRemoval(false)
                        .shutdownInProgress(false)
                        .utilization(10)
                        .cpuLoad(0.5)
                        .nodeTime(100.0)
                        .nodeCpuIdleTotal(100.0)
                        .nodeCpuCount(2)
                        .build(),
                JvbInstanceData.builder()
                        .id("JvbInstance2")
                        .version("v1")
                        .scheme("http")
                        .host("host2")
                        .port(8002)
                        .needShutdown(false)
                        .responding(false)
                        .respondedOnce(false)
                        .scheduledForRemoval(false)
                        .shutdownInProgress(false)
                        .utilization(null)
                        .notRespondingSince(LocalDateTime.MIN)
                        .build(),
                JvbInstanceData.builder()
                        .id("JvbInstance3")
                        .version("v1")
                        .scheme("http")
                        .host("host3")
                        .port(8003)
                        .responding(true)
                        .respondedOnce(true)
                        .scheduledForRemoval(false)
                        .shutdownInProgress(false)
                        .utilization(10)
                        .build()
        ))));
    }

    @Test
    public void stopNotRespondingTooLongInstancesTest() {
        JvbInstanceData jvbInstanceData1 = JvbInstanceData.builder()
                .id("JvbInstance1")
                .build();
        JvbInstanceData jvbInstanceData2 = JvbInstanceData.builder()
                .id("JvbInstance2")
                .build();

        LocalDateTime now = LocalDateTime.now();
        when(jvbInstanceDataRepository.findAllByNotRespondingSinceIsBeforeAndRespondedOnce(now.minusSeconds(5), true))
                .thenReturn(List.of(jvbInstanceData1));
        when(jvbInstanceDataRepository.findAllByNotRespondingSinceIsBeforeAndRespondedOnce(now.minusMinutes(5), false))
                .thenReturn(List.of(jvbInstanceData2));
        when(jvbInstanceManagementService.stop(eq(jvbInstanceData1.getId()))).thenReturn(true);
        when(jvbInstanceManagementService.stop(eq(jvbInstanceData2.getId()))).thenReturn(false);

        try(MockedStatic<LocalDateTime> ms = mockStatic(LocalDateTime.class)) {
            ms.when(LocalDateTime::now).thenReturn(now);
            jvbInstancesTasksService.stopNotRespondingTooLongInstances();
        }

        verify(jvbInstanceDataRepository).deleteAll(List.of(jvbInstanceData1));
    }

    @Test
    public void scaleInstancesTest_oneInstanceIsNotResponding() {
        when(jvbInstanceDataRepository.findAll()).thenReturn(List.of(
                JvbInstanceData.builder()
                        .id("JvbInstance1")
                        .responding(false)
                        .build(),
                JvbInstanceData.builder()
                        .id("JvbInstance2")
                        .responding(true)
                        .build()
        ));

        jvbInstancesTasksService.scaleInstances();

        verifyNoMoreInteractions(jvbInstanceDataRepository);
        verifyNoInteractions(jvbConferenceDataRepository);
        verifyNoInteractions(jvbInstanceManagementService);
    }

    @Test
    public void scaleInstancesTest_notEnoughJvbInstances() {
        when(jvbInstanceDataRepository.findAll()).thenReturn(List.of(
                JvbInstanceData.builder()
                        .id("JvbInstance")
                        .responding(true)
                        .scheduledForRemoval(true)
                        .build()
        ));

        jvbInstancesTasksService.scaleInstances();

        verify(jvbInstanceManagementService, times(JVB_MIN_POOL_SIZE)).start();
        verifyNoMoreInteractions(jvbInstanceManagementService);

        verify(jvbInstanceDataRepository).findAllByScheduledForRemovalIsTrueAndShutdownInProgressIsFalse();
        verifyNoMoreInteractions(jvbInstanceDataRepository);

        verifyNoInteractions(jvbConferenceDataRepository);
    }

}
