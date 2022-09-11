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
public class VideobridgeConferenceOffer {
    private String videobridgeId;
    private String conferenceId;
    private String ufrag;
    private String pwd;
    private List<VideobridgeFingerprint> fingerprints;
    private List<VideobridgeCandidate> candidates;
    private boolean rtcpMux;

    private String sctpConnectionId;
    private VideobridgePrimaryChannel primaryAudioChannel;
    private VideobridgePrimaryChannel primaryVideoChannel;

    private List<VideobridgeChannel> audioChannels;
    private List<VideobridgeChannel> videoChannels;

    private List<VideobridgeEndpoint> endpoints;
}
