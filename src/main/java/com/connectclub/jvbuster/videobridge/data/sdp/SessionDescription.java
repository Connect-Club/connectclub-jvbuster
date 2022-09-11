package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDescription {

    private Integer version;
    private Origin origin;
    private URI uri;
    private SessionName sessionName;
    @Builder.Default
    private List<String> emails = new ArrayList<>();
    @Builder.Default
    private List<String> phones = new ArrayList<>();
    private Connection connection;
    @Singular
    private List<Attribute> attributes;
    private Key key;
    @Builder.Default
    private List<Time> times = new ArrayList<>();
    @Singular
    private List<MediaDescription> medias;

    public StringBuilder appendDetailsOnly(StringBuilder stringBuilder) {
        stringBuilder.append("v=").append(version).append("\r\n");
        origin.append(stringBuilder);
        sessionName.append(stringBuilder);
        if (uri != null) {
            stringBuilder.append("u=").append(uri.toString()).append("\r\n");
        }
        for (String email : emails) {
            stringBuilder.append("e=").append(email).append("\r\n");
        }
        for (String phone : phones) {
            stringBuilder.append("p=").append(phone).append("\r\n");
        }
        if (connection != null) {
            connection.append(stringBuilder);
        }
        for (Time time : times) {
            time.append(stringBuilder);
        }
        for (Attribute attr : attributes) {
            attr.append(stringBuilder);
        }
        return stringBuilder;
    }

    public StringBuilder append(StringBuilder stringBuilder) {
        appendDetailsOnly(stringBuilder);
        for (MediaDescription media : medias) {
            media.append(stringBuilder);
        }
        return stringBuilder;
    }

    public String toDetailsOnlyString() {
        return appendDetailsOnly(new StringBuilder()).toString();
    }

}

