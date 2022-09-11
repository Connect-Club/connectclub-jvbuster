package com.connectclub.jvbuster.nodeexporter;

import com.connectclub.jvbuster.nodeexporter.i.NodeExporterService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NodeExporterTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private Call call;

    @Mock
    private Response response;

    @Mock
    private ResponseBody responseBody;

    private NodeExporterService nodeExporterService;

    @BeforeEach
    public void beforeClass() {
        nodeExporterService = new DefaultNodeExporterService(okHttpClient);
    }

    @Test
    public void getMetricsTest() throws IOException {
        when(okHttpClient.newCall(argThat(x -> x.url().host().equals("localhost")))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.charStream())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/node-exporter.response")));

        Map<String, String> metrics = nodeExporterService.getMetrics("localhost", List.of("node_time", "node_cpu\\{cpu=\"[a-zA-Z0-9]+\",mode=\"idle\"\\}"));
        assertEquals(
                Map.of(
                        "node_time", "1.608283216753724e+09",
                        "node_cpu{cpu=\"cpu0\",mode=\"idle\"}", "576506.7",
                        "node_cpu{cpu=\"cpu1\",mode=\"idle\"}", "574478.29"
                ), metrics
        );
    }

}
