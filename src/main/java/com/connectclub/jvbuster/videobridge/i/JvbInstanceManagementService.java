package com.connectclub.jvbuster.videobridge.i;

import com.connectclub.jvbuster.videobridge.JvbInstance;

import java.util.List;

public interface JvbInstanceManagementService {
    String start(boolean forSpeakers);

    boolean stop(String instanceId);

    List<JvbInstance> getActive();

    String getLastVersion();
}
