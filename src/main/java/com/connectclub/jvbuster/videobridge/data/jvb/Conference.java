package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conference {

    public final static Conference NULL = new Conference();

    public final static Type LIST_TYPE = new TypeToken<List<Conference>>() {}.getType();

    @SerializedName("id")
    private String id;

    @SerializedName("gid")
    private String gid;

    @SerializedName("contents")
    private List<Content> contents = Collections.emptyList();

    @SerializedName("channel-bundles")
    private List<ChannelBundle> channelBundles = Collections.emptyList();

    @SerializedName("endpoints")
    private List<Endpoint> endpoints = Collections.emptyList();

}
