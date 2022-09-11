package com.connectclub.jvbuster.repository.i;

import com.connectclub.jvbuster.repository.data.JvbEndpointData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JvbEndpointDataRepository extends JpaRepository<JvbEndpointData, String> {
}
