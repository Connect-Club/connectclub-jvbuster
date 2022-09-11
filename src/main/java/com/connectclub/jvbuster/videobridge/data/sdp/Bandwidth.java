package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bandwidth {

    private String type;
    private long bandwidth;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("b=").append(type).append(":").append(bandwidth).append("\r\n");
    }
}
