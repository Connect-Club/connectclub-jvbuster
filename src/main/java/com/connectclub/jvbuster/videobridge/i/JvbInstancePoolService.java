package com.connectclub.jvbuster.videobridge.i;


import com.connectclub.jvbuster.videobridge.JvbInstance;
import com.connectclub.jvbuster.videobridge.data.jvb.Conference;

import java.util.Map;

public interface JvbInstancePoolService {
    Map<JvbInstance, Conference> getInstances(String conferenceGid);
}
