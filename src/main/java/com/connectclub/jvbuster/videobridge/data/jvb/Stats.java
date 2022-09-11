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
public class Stats {

    @SerializedName("receive_only_endpoints")
    private int receiveOnlyEndpoints;

    @SerializedName("endpoints_sending_video")
    private int endpointsSendingVideo;

    @SerializedName("endpoints_sending_audio")
    private int endpointsSendingAudio;

    @SerializedName("graceful_shutdown")
    private boolean shutdownInProgress;

}
