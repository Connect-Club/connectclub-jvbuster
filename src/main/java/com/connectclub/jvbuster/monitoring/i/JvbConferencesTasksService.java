package com.connectclub.jvbuster.monitoring.i;

public interface JvbConferencesTasksService {

    void cacheConferences();

    void scaleConferences(boolean forSpeakers);

    void expireConferences();

    void syncConferences(String gid);
}
