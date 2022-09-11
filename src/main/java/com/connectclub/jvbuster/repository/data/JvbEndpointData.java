package com.connectclub.jvbuster.repository.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "JvbEndpoint")
public class JvbEndpointData {
    @Id
    private String id;

    private boolean speaker;

    @ManyToOne(optional = false)
    private JvbConferenceData conference;
}
