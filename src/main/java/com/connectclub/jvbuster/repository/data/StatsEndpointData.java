package com.connectclub.jvbuster.repository.data;

import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stats_endpoint", indexes = @Index(columnList = "conference_gid, endpoint_id"))
@ToString(exclude = "subscribedEndpoints")
public class StatsEndpointData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "conference_id")
    private String conferenceId;

    @Column(name = "conference_gid")
    private String conferenceGid;

    @Column(name = "endpoint_id")
    private String endpointId;

    @Column(name = "endpoint_uuid")
    private UUID endpointUuid;

    @Column(name = "rtt")
    private double rtt;

    @Column(name = "jitter")
    private double jitter;

    @Column(name = "expected_packets")
    private int expectedPackets;

    @Column(name = "fraction_lost")
    private byte fractionLost;

    @OneToMany(mappedBy = "statsEndpoint", fetch = FetchType.EAGER)
    private List<StatsSubscribedEndpointData> subscribedEndpoints;
}
