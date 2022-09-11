package com.connectclub.jvbuster.videobridge.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideobridgeFingerprint {
    private String value;
    private String hash;
    private String setup;
}
