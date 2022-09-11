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
public class Fingerprint {

    @SerializedName("fingerprint")
    private String fingerprint;

    @SerializedName("setup")
    private String setup;

    @SerializedName("hash")
    private String hash;

}
