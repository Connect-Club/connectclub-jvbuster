package com.connectclub.jvbuster.repository.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stats_subscribed_endpoint")
public class StatsSubscribedEndpointData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private StatsEndpointData statsEndpoint;

    @Column(name = "endpoint_id")
    private String endpointId;

    @Column(name = "expected_packets")
    private int expectedPackets;

    @Column(name = "fraction_lost")
    private byte fractionLost;
}
