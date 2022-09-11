package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.videobridge.data.jvb.Stats;
import com.connectclub.jvbuster.videobridge.i.JvbInstanceUtilsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class DefaultJvbInstanceUtilsService implements JvbInstanceUtilsService {

    @Override
    public double getUtilization(JvbInstance jvbInstance) {
        try {
            Stats stats = jvbInstance.getStats();
        } catch (Exception e) {
            log.info("Can not get videobridge statictics", e);
            return -1;
        }

        return 0;
    }

}
