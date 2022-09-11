package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Key {

    private String type;
    private String key;

    public StringBuilder append(StringBuilder stringBuilder) {
        stringBuilder.append("k=");
        if (key != null) {
            stringBuilder.append(key).append(" ");
        }
        return stringBuilder.append(type).append("\r\n");
    }

}
