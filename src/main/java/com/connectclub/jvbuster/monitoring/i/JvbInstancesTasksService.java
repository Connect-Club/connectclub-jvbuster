package com.connectclub.jvbuster.monitoring.i;

public interface JvbInstancesTasksService {

    void cacheInstances();

    void stopNotRespondingTooLongInstances();

    void scaleInstances(boolean forSpeakers);
}
