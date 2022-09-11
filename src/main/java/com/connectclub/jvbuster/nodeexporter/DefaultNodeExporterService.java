package com.connectclub.jvbuster.nodeexporter;

import com.connectclub.jvbuster.nodeexporter.i.NodeExporterService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultNodeExporterService implements NodeExporterService {

    private final OkHttpClient okHttpClient;

    public DefaultNodeExporterService(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public Map<String, String> getMetrics(String host, Collection<String> metricsPattern) throws IOException {
        Map<String, String> result = new HashMap<>();
        Request request = new Request.Builder()
                .url("http://" + host + ":9100/metrics")
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("code = {}, body = {}", response.code(), response.body().string());
                throw new RuntimeException("Unsuccessful http response");
            }
            List<Pattern> metricsCompiledPattern = metricsPattern.stream()
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
            try (BufferedReader reader = new BufferedReader(response.body().charStream())) {
                String line = reader.readLine();
                if (line == null) throw new RuntimeException("Node exporter response is empty");
                do {
                    if (line.startsWith("#")) continue;
                    String[] keyValue = line.split(" (?=\\S+$)");
                    if (keyValue.length != 2) throw new RuntimeException("Unsupported format");
                    if (metricsCompiledPattern.stream().anyMatch(x -> x.matcher(keyValue[0]).matches())) {
                        result.put(keyValue[0], keyValue[1]);
                    }
                } while ((line = reader.readLine()) != null);
            }
        }
        return result;
    }
}
