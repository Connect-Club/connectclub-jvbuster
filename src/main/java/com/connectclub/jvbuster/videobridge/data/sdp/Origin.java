package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Origin {

    private String username;
    private Long sessId;
    private Long sessVersion;
    private String nettype;
    private String addrtype;
    private String address;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("o=")
                .append(username).append(" ")
                .append(sessId).append(" ")
                .append(sessVersion).append(" ")
                .append(nettype).append(" ")
                .append(addrtype).append(" ")
                .append(address).append("\r\n");
    }

}
