package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.videobridge.data.VideobridgeChannel;
import com.connectclub.jvbuster.videobridge.data.VideobridgeFingerprint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideobridgeConferenceAnswer {
    private String videobridgeId;
    private String conferenceId;
    private String ufrag;
    private String pwd;
    private List<VideobridgeFingerprint> fingerprints;
    private boolean rtcpMux;

    private String sctpConnectionId;
    private VideobridgeChannel primaryAudioChannel;
    private VideobridgeChannel primaryVideoChannel;

}
