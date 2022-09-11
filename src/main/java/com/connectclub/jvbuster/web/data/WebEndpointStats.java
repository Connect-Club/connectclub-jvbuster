package com.connectclub.jvbuster.web.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WebEndpointStats {
    private Long id;
    private long createdAt;
    private double rtt;
    private int fractionLost;
    private List<WebEndpointStats.SubscribedEndpoint> subscribedEndpoints;

    @Data
    @Builder
    public static class SubscribedEndpoint {
        private String endpointId;
        private int fractionLost;
    }
}
