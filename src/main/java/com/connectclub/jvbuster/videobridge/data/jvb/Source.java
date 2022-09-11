package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    @SerializedName("endpointId")
    private String endpointId;

    @SerializedName("endpointUuid")
    private UUID endpointUuid;

    @SerializedName("ssrc")
    private long ssrc;

}
