package com.connectclub.jvbuster.repository.i;

import com.connectclub.jvbuster.repository.data.StatsEndpointData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatsEndpointDataRepository extends JpaRepository<StatsEndpointData, Long> {
    List<StatsEndpointData> findAllByConferenceGidOrderByCreatedAt(String conferenceGid);
    List<StatsEndpointData> findAllByConferenceGidAndEndpointIdOrderByCreatedAt(String conferenceGid, String endpointId);
}
