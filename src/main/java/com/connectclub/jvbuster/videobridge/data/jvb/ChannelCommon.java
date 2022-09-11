package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.List;

@Data
@SuperBuilder
public abstract class ChannelCommon {

    @SerializedName("id")
    private String id;

    @SerializedName("expire")
    private int expire;

    @SerializedName("payload-types")
    private List<PayloadType> payloadTypes = Collections.emptyList();

    @SerializedName("rtp-hdrexts")
    private List<RtpHdrext> rtpHdrexts = Collections.emptyList();

    @SerializedName("ssrc-groups")
    private List<SsrcGroup> ssrcGroups = Collections.emptyList();

    public ChannelCommon(){}
}
