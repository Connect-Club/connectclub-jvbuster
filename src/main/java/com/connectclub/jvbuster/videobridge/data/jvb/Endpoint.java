package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {

    @SerializedName("id")
    private String id;

    @SerializedName("uuid")
    private UUID uuid;

    @SerializedName("displayname")
    private String displayName;

}
