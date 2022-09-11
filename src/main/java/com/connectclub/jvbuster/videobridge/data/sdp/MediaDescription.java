package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDescription {

    private String media;
    private Integer port;
    private Integer numberOfPorts;
    @Builder.Default
    private List<String> proto = new ArrayList<>();
    @Singular
    private List<Integer> formats;
    private Information information;
    @Builder.Default
    private List<Connection> connections = new ArrayList<>();
    private Bandwidth bandwidth;
    @Singular
    private List<Attribute> attributes;
    private Key key;

    public StringBuilder append(StringBuilder stringBuilder) {
        stringBuilder.append("m=").append(media).append(" ").append(port);
        if (numberOfPorts != null) {
            stringBuilder.append("/").append(numberOfPorts);
        }
        stringBuilder.append(" ");
        boolean first = true;
        for (String p : proto) {
            if (!first) {
                stringBuilder.append("/");
            } else {
                first = false;
            }
            stringBuilder.append(p);
        }
        for (Integer f : formats) {
            stringBuilder.append(" ").append(f);
        }
        stringBuilder.append("\r\n");
        if (information != null) {
            information.append(stringBuilder);
        }
        for (Connection c : connections) {
            c.append(stringBuilder);
        }
        if (bandwidth != null) {
            bandwidth.append(stringBuilder);
        }
        if (key != null) {
            key.append(stringBuilder);
        }
        for (Attribute b : attributes) {
            b.append(stringBuilder);
        }
        return stringBuilder;
    }
}
