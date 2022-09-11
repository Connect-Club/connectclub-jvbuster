package com.connectclub.jvbuster.videobridge.i;

import com.connectclub.jvbuster.videobridge.VideobridgeConferenceAnswer;
import com.connectclub.jvbuster.videobridge.data.VideobridgeConferenceOffer;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;

import java.io.IOException;
import java.util.List;

public interface NewVideobridgeService {

    List<VideobridgeConferenceOffer> getNewOffers(String conferenceGid, String endpoint, boolean speaker) throws IOException, JvbInstanceRestException;

    List<VideobridgeConferenceOffer> getCurrentOffers(String conferenceGid, String endpoint) throws JvbInstanceRestException, IOException;

    void processAnswers(String conferenceGid, String endpoint, List<VideobridgeConferenceAnswer> conferences) throws IOException, JvbInstanceRestException;

    void delete(String conferenceGid, String endpoint, boolean quiet);
}
