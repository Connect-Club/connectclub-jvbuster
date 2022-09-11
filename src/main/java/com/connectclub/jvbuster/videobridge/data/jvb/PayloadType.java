package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadType {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("clockrate")
    private long clockrate;

    @SerializedName("channels")
    private int channels;

    @SerializedName("parameters")
    private Map<String,Object> parameters = Collections.emptyMap();

    @SerializedName("rtcp-fbs")
    private List<RtcpFb> rtcpFbs = Collections.emptyList();
}
