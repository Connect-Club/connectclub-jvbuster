package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @SerializedName("name")
    private String name;

    @Singular
    @SerializedName("channels")
    private List<ChannelCommon> channels = Collections.emptyList();

    @Singular
    @SerializedName("sctpconnections")
    private List<SctpConnection> sctpConnections = Collections.emptyList();

}
