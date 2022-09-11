package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAttribute extends Attribute {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Extension {
        public String name;
        public String value;
    }

    private String foundation;
    private Integer componentId;
    private String transport;
    private Integer priority;
    private String address;
    private Integer port;
    private String type;
    private String relAddr;
    private Integer relPort;
    @Builder.Default
    private List<Extension> extensions = new ArrayList<>();

    @Override
    public String getField() {
        return "candidate";
    }

    @Override
    public String getValue() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(foundation).append(" ")
                .append(componentId).append(" ")
                .append(transport).append(" ")
                .append(priority).append(" ")
                .append(address).append(" ")
                .append(port).append(" typ ").append(type);
        if (relAddr != null) {
            stringBuilder.append(" raddr ").append(relAddr);
        }
        if (relPort != null) {
            stringBuilder.append(" rport ").append(relPort);
        }
        for (Extension ext : extensions) {
            stringBuilder.append(" ").append(ext.name).append(" ").append(ext.value);
        }
        return stringBuilder.toString();
    }

}
