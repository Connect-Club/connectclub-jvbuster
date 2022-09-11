package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceUfragAttribute extends Attribute {

    private String ufrag;

    @Override
    public String getField() {
        return "ice-ufrag";
    }

    @Override
    public String getValue() {
        return ufrag;
    }
}
