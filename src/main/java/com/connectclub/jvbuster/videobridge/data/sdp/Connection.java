package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connection {

    private String netType;
    private String addrType;
    private String address;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("c=")
                .append(netType).append(" ")
                .append(addrType).append(" ")
                .append(address).append("\r\n");
    }

}
