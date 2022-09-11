package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionName {

    private String name;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("s=").append(name).append("\r\n");
    }

}
