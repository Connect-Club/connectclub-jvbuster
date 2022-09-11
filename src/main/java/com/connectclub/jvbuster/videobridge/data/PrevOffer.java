package com.connectclub.jvbuster.videobridge.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class PrevOffer {
    private final String conferenceId;
    private final long sessionId;
    private final long sessionVersion;
    private final List<String> channels;
}