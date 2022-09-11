package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeControlProtocolAttribute extends Attribute {

    private int port;
    private String netType;
    private String addrType;
    private String address;

    @Override
    public String getField() {
        return "rtcp";
    }

    @Override
    public String getValue() {
        return port + " " + netType + " " + addrType + " " + address;
    }
}
