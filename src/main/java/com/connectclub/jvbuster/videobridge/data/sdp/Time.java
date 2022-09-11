package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Time {

    private Integer start;
    private Integer stop;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("t=").append(start).append(" ").append(stop).append("\r\n");
    }

}
