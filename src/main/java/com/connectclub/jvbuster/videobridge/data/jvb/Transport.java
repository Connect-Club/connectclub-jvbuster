package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transport {

    @SerializedName("candidates")
    private List<Candidate> candidates = Collections.emptyList();

    @SerializedName("xmlns")
    private String xmlns;

    @SerializedName("rtcp-mux")
    private boolean rtcpMux;

    @SerializedName("ufrag")
    private String ufrag;

    @SerializedName("pwd")
    private String pwd;

    @SerializedName("fingerprints")
    private List<Fingerprint> fingerprints = Collections.emptyList();

}
