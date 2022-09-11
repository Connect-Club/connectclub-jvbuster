package com.connectclub.jvbuster.repository.i;

import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JvbInstanceDataRepository extends JpaRepository<JvbInstanceData, String> {

    List<JvbInstanceData> findAllByForSpeakers(boolean forSpeakers);

    List<JvbInstanceData> findAllByRespondingIsTrue();

    List<JvbInstanceData> findAllByScheduledForRemovalIsTrueAndShutdownInProgressIsFalseAndForSpeakers(boolean forSpeakers);

    List<JvbInstanceData> findAllByIdInOrderByUtilization(Iterable<String> ids);

    List<JvbInstanceData> findAllByNotRespondingSinceIsBeforeAndRespondedOnce(LocalDateTime dt, boolean respondedOnce);

    Optional<JvbInstanceData> findFirstByRespondingIsTrueAndScheduledForRemovalIsFalseAndShutdownInProgressIsFalseAndForSpeakersOrderByUtilization(boolean forSpeakers);

    List<JvbInstanceData> findAllByIdIsNotIn(Iterable<String> ids);
}
