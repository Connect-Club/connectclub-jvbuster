package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintAttribute extends Attribute {

    private String hashFunc;
    private String fingerprint;

    @Override
    public String getField() {
        return "fingerprint";
    }

    @Override
    public String getValue() {
        return hashFunc + " " + fingerprint;
    }

}
