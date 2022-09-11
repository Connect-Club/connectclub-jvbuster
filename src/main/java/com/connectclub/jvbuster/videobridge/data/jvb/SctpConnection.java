package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SctpConnection {

    @SerializedName("id")
    private String id;

    @SerializedName("expire")
    private int expire;

    @SerializedName("initiator")
    private boolean initiator;

    @SerializedName("endpoint")
    private String endpoint;

    @SerializedName("port")
    private Integer port;

    @SerializedName("channel-bundle-id")
    private String channelBundleId;
}
