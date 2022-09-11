package com.connectclub.jvbuster;

import com.connectclub.jvbuster.monitoring.i.JvbConferencesTasksService;
import com.connectclub.jvbuster.monitoring.i.JvbInstancesTasksService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScheduledTasksService {

    private final JvbInstancesTasksService jvbInstancesTasksService;

    private final JvbConferencesTasksService jvbConferencesTasksService;

    private final RAtomicLong cacheConferencesLaunchTime;
    private final RAtomicLong expireConferenceLaunchTime;
    private final RAtomicLong stopAndScaleInstancesLaunchTime;

    public ScheduledTasksService(
            JvbInstancesTasksService jvbInstancesTasksService,
            JvbConferencesTasksService jvbConferencesTasksService,
            RedissonClient redissonClient
    ) {
        this.jvbInstancesTasksService = jvbInstancesTasksService;
        this.jvbConferencesTasksService = jvbConferencesTasksService;
        cacheConferencesLaunchTime = redissonClient.getAtomicLong("CacheConferencesLaunchTime");
        expireConferenceLaunchTime = redissonClient.getAtomicLong("ExpireConferenceLaunchTime");
        stopAndScaleInstancesLaunchTime = redissonClient.getAtomicLong("StopAndScaleInstancesLaunchTime");
    }

    @Scheduled(cron = "*/1 * * * * *")
    @SchedulerLock(name = "cacheAndMonitor")
    public void cacheAndMonitor() {
        try {
            jvbInstancesTasksService.cacheInstances();

            if(System.currentTimeMillis() - cacheConferencesLaunchTime.get() > 30_000) {
                jvbConferencesTasksService.cacheConferences();
                cacheConferencesLaunchTime.set(System.currentTimeMillis());
            }

            if(System.currentTimeMillis() - expireConferenceLaunchTime.get() > 60_000) {
                jvbConferencesTasksService.expireConferences();
                expireConferenceLaunchTime.set(System.currentTimeMillis());
            }

            jvbConferencesTasksService.scaleConferences(true);
            jvbConferencesTasksService.scaleConferences(false);

            if (System.currentTimeMillis() - stopAndScaleInstancesLaunchTime.get() > 5_000) {
                jvbInstancesTasksService.stopNotRespondingTooLongInstances();
                jvbInstancesTasksService.scaleInstances(true);
                jvbInstancesTasksService.scaleInstances(false);
                stopAndScaleInstancesLaunchTime.set(System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error("cacheAndMonitor exception", e);
        }
    }
}
