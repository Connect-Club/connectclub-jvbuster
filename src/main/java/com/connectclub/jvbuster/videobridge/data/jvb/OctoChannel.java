package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OctoChannel extends ChannelCommon {

    @SerializedName("type")
    private final String type = "octo";

    @SerializedName("sources")
    private List<Source> sources = Collections.emptyList();

    @SerializedName("relays")
    private Set<String> relays = Collections.emptySet();

//    @SerializedName("rtp-hdrexts")
//    private List<RtpHdrext> rtpHdrexts;
//
//    @SerializedName("payload-types")
//    private List<PayloadType> payloadTypes;
//
//    @SerializedName("sources")
//    private List<Long> sources;
//
//    @SerializedName("ssrcs")
//    private List<Long> ssrcs;
//
//    @SerializedName("ssrc-groups")
//    private List<SsrcGroup> ssrcGroups;
}
