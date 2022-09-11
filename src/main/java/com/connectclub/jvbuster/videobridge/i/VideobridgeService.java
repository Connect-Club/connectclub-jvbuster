package com.connectclub.jvbuster.videobridge.i;

import com.connectclub.jvbuster.videobridge.data.Answer;
import com.connectclub.jvbuster.videobridge.data.PrevOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.Candidate;
import com.connectclub.jvbuster.videobridge.data.sdp.SessionDescription;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;

import java.io.IOException;
import java.util.List;

public interface VideobridgeService {
    List<SessionDescription> getOffers(String conferenceGid, String endpoint, Long videoBandwidth, String... videobridgeIds) throws IOException, JvbInstanceRestException;

    List<SessionDescription> getOffers(String conferenceGid, String endpoint, Long videoBandwidth, List<PrevOffer> prevOffers);

    void processAnswers(String conferenceGid, String endpoint, List<Answer> answers) throws IOException, JvbInstanceRestException;

    void processIceCandidate(
            String conferenceGid,
            String endpoint,
            String conferenceId,
            String foundation,
            int component,
            Candidate.Protocol protocol,
            int priority,
            String ip,
            int port,
            Candidate.Type type,
            String relAddr,
            Integer relPort,
            String generation
    );

    void delete(String conferenceGid, String endpoint, boolean quiet, String... videobridgeIds);
}
