package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupAttribute extends Attribute {

    private String role;

    @Override
    public String getField() {
        return "setup";
    }

    @Override
    public String getValue() {
        return role;
    }
}
