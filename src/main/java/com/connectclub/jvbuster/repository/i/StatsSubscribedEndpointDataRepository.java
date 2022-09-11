package com.connectclub.jvbuster.repository.i;

import com.connectclub.jvbuster.repository.data.StatsSubscribedEndpointData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsSubscribedEndpointDataRepository extends JpaRepository<StatsSubscribedEndpointData, Long> {
}
