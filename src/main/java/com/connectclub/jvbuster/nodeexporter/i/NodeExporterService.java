package com.connectclub.jvbuster.nodeexporter.i;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface NodeExporterService {
    Map<String, String> getMetrics(String host, Collection<String> metricsPattern) throws IOException;
}
