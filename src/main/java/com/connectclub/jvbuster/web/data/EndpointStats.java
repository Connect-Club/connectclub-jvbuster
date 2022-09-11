package com.connectclub.jvbuster.web.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EndpointStats {
    @Data
    @Builder
    public static class SubscribedEndpoint {
        private String endpointId;
        private int expectedPackets;
        private int fractionLost;
    }

    private long createdAt;
    private String conferenceId;
    private String conferenceGid;
    private String endpointId;
    private UUID endpointUuid;
    private double rtt;
    private double jitter;
    private int expectedPackets;
    private int fractionLost;
    private List<SubscribedEndpoint> subscribedEndpoints;
}
