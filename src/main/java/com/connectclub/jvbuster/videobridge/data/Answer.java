package com.connectclub.jvbuster.videobridge.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Answer {

    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class Candidate {
        private final String foundation;
        private final String componentId;
        private final String transport;
        private final String priority;
        private final String connectionAddress;
        private final String port;
        private final String candidateType;
        private final String relAddress;
        private final String relPort;
        private final String generation;
    }

    private final String conferenceId;

    private final String sctpConnectionId;

    private final String audioChannelId;
    private final List<Long> audioSsrc;
    private final List<Map.Entry<String, List<Long>>> audioSsrcGroups;

    private final String videoChannelId;
    private final List<Long> videoSsrc;
    private final List<Map.Entry<String, List<Long>>> videoSsrcGroups;

    private final String fingerprintValue;
    private final String fingerprintHash;
    private final String fingerprintSetup;

    private final String iceUfrag;
    private final String icePwd;

    private final List<Candidate> candidates;
}
