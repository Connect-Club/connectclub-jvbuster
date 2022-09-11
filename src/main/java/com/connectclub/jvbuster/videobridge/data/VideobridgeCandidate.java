package com.connectclub.jvbuster.videobridge.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideobridgeCandidate {
    private String foundation;
    private Integer component;
    private String protocol;
    private Integer priority;
    private String ip;
    private Integer port;
    private String type;
    private String relAddr;
    private Integer relPort;
    private String generation;
}
