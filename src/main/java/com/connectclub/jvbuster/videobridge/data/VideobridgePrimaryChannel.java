package com.connectclub.jvbuster.videobridge.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideobridgePrimaryChannel {
    private String id;
    private String direction;
    private List<Long> sources;
}
