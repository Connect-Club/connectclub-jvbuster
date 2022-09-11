package com.connectclub.jvbuster.repository.data;

import lombok.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "JvbInstance")
@ToString(exclude = "conferences")
public class JvbInstanceData {
    @Id
    private String id;
    private String version;
    private String scheme;
    private String host;
    private int port;

    private Double nodeTime;
    private Double nodeCpuIdleTotal;
    private Integer nodeCpuCount;
    private Double cpuLoad;

    private boolean forSpeakers;
    private Integer octoBindPort;
    private Integer debuggerPort;

    private boolean needShutdown;

    private boolean responding;

    private boolean respondedOnce;

    //----------responding==true----------
    private boolean scheduledForRemoval;

    private boolean shutdownInProgress;

    private Integer utilization;
    //------------------------------------

    //----------responding==false---------
    private LocalDateTime notRespondingSince;
    //------------------------------------

    @OneToMany(mappedBy = "instance", cascade = CascadeType.REMOVE)
    private List<JvbConferenceData> conferences;
}
