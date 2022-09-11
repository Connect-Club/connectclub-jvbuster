package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Channel extends ChannelCommon {

    public enum Direction {
        @SerializedName("sendrecv") SENDRECV,
        @SerializedName("recvonly") RECVONLY,
        @SerializedName("sendonly") SENDONLY
    }

    public enum RtpLevelRelayType {
        @SerializedName("mixer") MIXER,
        @SerializedName("translator") TRANSLATOR
    }

    @SerializedName("initiator")
    private boolean initiator;

    @SerializedName("endpoint")
    private String endpoint;

    @SerializedName("direction")
    private Direction direction;

    @SerializedName("channel-bundle-id")
    private String channelBundleId;

    @SerializedName("rtp-level-relay-type")
    private RtpLevelRelayType rtpLevelRelayType;

    //todo: remove when simulcast be on the prod
    @SerializedName("last-n")
    private Integer lastN;

    @SerializedName("sources")
    private List<Long> sources = Collections.emptyList();

    //заполняется только сервером
    @SerializedName("ssrcs")
    private List<Long> ssrcs = Collections.emptyList();

}
