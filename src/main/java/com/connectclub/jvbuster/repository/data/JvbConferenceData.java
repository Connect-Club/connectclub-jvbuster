package com.connectclub.jvbuster.repository.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "JvbConference")
public class JvbConferenceData {
    @Id
    private String id;

    private String gid;

    private String confId;

    @ManyToOne(optional = false)
    private JvbInstanceData instance;

    @OneToMany(mappedBy = "conference", cascade = CascadeType.REMOVE)
    private List<JvbEndpointData> endpoints;

    public static String buildId(String gid, String confId) {
        return String.format("%s-%s", gid, confId);
    }

}
