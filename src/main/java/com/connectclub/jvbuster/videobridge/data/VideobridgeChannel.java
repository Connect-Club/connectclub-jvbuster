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
public class VideobridgeChannel {
    private String id;
    private String endpoint;
    private List<Long> ssrcs;
    private List<VideobridgeSsrcGroup> ssrcGroups;
}
