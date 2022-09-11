package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Information {

    private String text;

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder.append("i=").append(text).append("\r\n");
    }

}
