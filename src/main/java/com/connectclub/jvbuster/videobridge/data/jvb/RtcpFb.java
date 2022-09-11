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
public class RtcpFb {

    @SerializedName("type")
    private String type;

    @SerializedName("subtype")
    private String subtype;

}
