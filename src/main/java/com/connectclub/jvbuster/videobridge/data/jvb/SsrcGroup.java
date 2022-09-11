package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "build")
public class SsrcGroup {

    @SerializedName("semantics")
    private String semantics;

    @SerializedName("sources")
    private List<Long> sources = Collections.emptyList();
}
