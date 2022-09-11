package com.connectclub.jvbuster.repository.i;

import com.connectclub.jvbuster.repository.data.JvbConferenceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JvbConferenceDataRepository extends JpaRepository<JvbConferenceData, String> {

    List<JvbConferenceData> findAllByInstanceId(String instanceId);

    List<JvbConferenceData> findAllByGid(String gid);

    List<JvbConferenceData> findAllByGidAndInstanceForSpeakers(String gid, boolean forSpeaker);

    List<JvbConferenceData> findAllByInstanceForSpeakers(boolean forSpeaker);

    Optional<JvbConferenceData> findByGidAndInstanceId(String gid, String instanceId);

    int countByInstanceId(String instanceId);

    List<JvbConferenceData> findAllByGidAndInstanceIdIn(String gid, Iterable<String> instanceIds);

    List<JvbConferenceData> findAllByIdIsNotIn(Iterable<String> ids);

    void deleteAllByInstanceIdIn(Iterable<String> instanceId);

    void deleteAllByIdNotIn(Iterable<String> ids);
}
