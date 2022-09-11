package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcePasswordAttribute extends Attribute {

    private String password;

    @Override
    public String getField() {
        return "ice-pwd";
    }

    @Override
    public String getValue() {
        return password;
    }
}
