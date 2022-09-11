package com.connectclub.jvbuster.web;

import com.connectclub.jvbuster.JvbusterApplicationTest;
import com.connectclub.jvbuster.SessionDescriptionTest;
import com.connectclub.jvbuster.videobridge.data.Answer;
import com.connectclub.jvbuster.videobridge.data.PrevOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.Candidate;
import com.connectclub.jvbuster.videobridge.data.sdp.Origin;
import com.connectclub.jvbuster.videobridge.data.sdp.SessionDescription;
import com.connectclub.jvbuster.videobridge.data.sdp.SessionName;
import com.connectclub.jvbuster.videobridge.i.VideobridgeService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = JvbusterApplicationTest.class)
@WebMvcTest
public class SignalingControllerTest {

    private final static String jwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbmRwb2ludCI6InRlc3QtZW5kcG9pbnQiLCJjb25mZXJlbmNlR2lkIjoidGVzdC1jb25mZXJlbmNlIn0.GzoDWx1w74meirOPKWyrHkyUGXb-oHJbQlAIZ9MyL0iFLaAh9ZNbb5II48lDRkhN136HVoPtnMW4gS3u_28j13pcY0VRFP0PjtuS42rxUzjqHn2YOp0Qbnkr-Ctb0ganNpZO3SzVr6QAXqOmoBrS71Mnct1v50tw03c8bCS0Rex4QQrA_btN4SCzCp4Ii2jE_cNWBO2Q5h0ZLbHHMmMR_zUQve9PZL8NNOIuLBlE_PYQDBQNR3IBGrHOPHEmDLMJ5mjadc93YedIRHO5175IN0erukkb_HA9bD4G4dc7v2h9Ttmny_v0ixkZ6l8x6ZaQ0EtrvRS7n9qW7tCws8OKpw";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private VideobridgeService videobridgeService;

    @MockBean
    private RLock rLock;

    @Test
    public void testGetOffers() throws Exception {
        when(redissonClient.getFairLock("getOffers-test-conference-test-endpoint")).thenReturn(rLock);

        when(rLock.tryLock()).thenReturn(true);

        when(videobridgeService.getOffers("test-conference", "test-endpoint"))
                .thenReturn(List.of(
                        SessionDescription.builder()
                                .version(0)
                                .origin(Origin.builder()
                                        .username("-")
                                        .sessId(1605218513L)
                                        .sessVersion(1L)
                                        .nettype("IN")
                                        .addrtype("IP4")
                                        .address("0.0.0.0")
                                        .build())
                                .sessionName(new SessionName("-"))
                                .build(),
                        SessionDescription.builder()
                                .version(0)
                                .origin(Origin.builder()
                                        .username("-")
                                        .sessId(1605218514L)
                                        .sessVersion(1L)
                                        .nettype("IN")
                                        .addrtype("IP4")
                                        .address("0.0.0.0")
                                        .build())
                                .sessionName(new SessionName("-"))
                                .build()
                ));

        mockMvc.perform(
                get("/signaling/offers")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
        ).andExpect(status().isOk()).andExpect(content().string(
                "v=0\r\n" +
                        "o=- 1605218513 1 IN IP4 0.0.0.0\r\n" +
                        "s=-\r\n" +
                        "\r\n" +
                        "v=0\r\n" +
                        "o=- 1605218514 1 IN IP4 0.0.0.0\r\n" +
                        "s=-\r\n"));

        verify(rLock).unlock();
    }

    @Test
    public void testGetOffers2() throws Exception {
        when(redissonClient.getFairLock("getOffers-test-conference-test-endpoint")).thenReturn(rLock);

        when(rLock.tryLock()).thenReturn(false);

        mockMvc.perform(
                get("/signaling/offers")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
        ).andExpect(status().isConflict())
                .andExpect(content().string("com.connectclub.jvbuster.exception.ConflictException: Multiple GET offers from the same endpoint is not allowed"));

        verify(rLock, never()).unlock();
    }

    @Test
    public void testGetOffer() throws Exception {
        when(videobridgeService.getOffers("test-conference", "test-endpoint", "videobridgeId"))
                .thenReturn(List.of(
                        SessionDescription.builder()
                                .version(0)
                                .origin(Origin.builder()
                                        .username("-")
                                        .sessId(1605218513L)
                                        .sessVersion(1L)
                                        .nettype("IN")
                                        .addrtype("IP4")
                                        .address("0.0.0.0")
                                        .build())
                                .sessionName(new SessionName("-"))
                                .build()
                ));

        mockMvc.perform(
                get("/signaling/offer/videobridgeId")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
        ).andExpect(status().isOk()).andExpect(content().string(
                "v=0\r\n" +
                        "o=- 1605218513 1 IN IP4 0.0.0.0\r\n" +
                        "s=-\r\n"));
    }

    @Test
    public void testPostAnswers() throws Exception {
        mockMvc.perform(
                post("/signaling/answers")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
                        .content("v=0\r\n" +
                                "o=- 7400803269221096182 3 IN IP4 127.0.0.1\r\n" +
                                "s=-\r\n" +
                                "t=0 0\r\n" +
                                "a=group:BUNDLE data-f05ee38194f1d722-d1a850e0b951f1cb audio-mixed-5d3d40a6648d7ec7 video-mixed-76f4ed32e2a32f0a\r\n" +
                                "a=msid-semantic: WMS kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH\r\n" +
                                "m=application 29995 DTLS/SCTP 5000\r\n" +
                                "c=IN IP4 93.81.219.123\r\n" +
                                "b=AS:30\r\n" +
                                "a=candidate:2955402076 1 udp 2122260223 192.168.0.179 49187 typ host generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:829414888 1 udp 1686052607 93.81.219.123 29995 typ srflx raddr 192.168.0.179 rport 49187 generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:4272170924 1 tcp 1518280447 192.168.0.179 9 typ host tcptype active generation 0 network-id 1 network-cost 10\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:data-f05ee38194f1d722-d1a850e0b951f1cb\r\n" +
                                "a=sctpmap:5000 webrtc-datachannel 1024\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-mixed-5d3d40a6648d7ec7\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=sendonly\r\n" +
                                "a=msid:kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH 5adaf9ec-d058-4650-b216-a10418e173f5\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n" +
                                "a=ssrc:333445445 cname:SWPtyKkVQTBillVh\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-mixed-76f4ed32e2a32f0a\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=sendonly\r\n" +
                                "a=msid:kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH 8554d878-e7f1-4c88-b195-d1130bcb476f\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "a=ssrc-group:FID 950630992 950630993\r\n" +
                                "a=ssrc:950630992 cname:SWPtyKkVQTBillVh\r\n" +
                                "a=ssrc:950630993 cname:SWPtyKkVQTBillVh\r\n" +
                                "\r\n" +
                                "v=0\r\n" +
                                "o=- 8445871945891786833 3 IN IP4 127.0.0.1\r\n" +
                                "s=-\r\n" +
                                "t=0 0\r\n" +
                                "a=group:BUNDLE data-b6eef289d220c9c7-5ec9d859fdf20211 audio-fake video-fake video-333ad77887c4a400 audio-ff946d001a80c23\r\n" +
                                "a=msid-semantic: WMS\r\n" +
                                "m=application 33429 DTLS/SCTP 5000\r\n" +
                                "c=IN IP4 93.81.219.123\r\n" +
                                "b=AS:30\r\n" +
                                "a=candidate:2955402076 1 udp 2122260223 192.168.0.179 53515 typ host generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:829414888 1 udp 1686052607 93.81.219.123 33429 typ srflx raddr 192.168.0.179 rport 53515 generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:4272170924 1 tcp 1518280447 192.168.0.179 9 typ host tcptype active generation 0 network-id 1 network-cost 10\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:data-b6eef289d220c9c7-5ec9d859fdf20211\r\n" +
                                "a=sctpmap:5000 webrtc-datachannel 1024\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-fake\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=inactive\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-fake\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=inactive\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-333ad77887c4a400\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=recvonly\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-ff946d001a80c23\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=recvonly\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n")
        );

        verify(videobridgeService).processAnswers(
                "test-conference",
                "test-endpoint",
                List.of(
                        Answer.builder()
                                .conferenceId("f05ee38194f1d722")
                                .sctpConnectionId("d1a850e0b951f1cb")
                                .audioChannelId("5d3d40a6648d7ec7")
                                .audioSsrc(List.of(333445445L))
                                .audioSsrcGroups(List.of())
                                .videoChannelId("76f4ed32e2a32f0a")
                                .videoSsrc(List.of(950630992L, 950630993L))
                                .videoSsrcGroups(List.of(Map.entry("FID", List.of(950630992L, 950630993L))))
                                .fingerprintValue("E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02")
                                .fingerprintHash("sha-256")
                                .fingerprintSetup("active")
                                .iceUfrag("S1Dd")
                                .icePwd("xXCto3PhAU6vjv5IEJdOeqV4")
                                .build(),
                        Answer.builder()
                                .conferenceId("b6eef289d220c9c7")
                                .sctpConnectionId("5ec9d859fdf20211")
                                .audioChannelId(null)
                                .audioSsrc(List.of())
                                .audioSsrcGroups(List.of())
                                .videoChannelId(null)
                                .videoSsrc(List.of())
                                .videoSsrcGroups(List.of())
                                .fingerprintValue("65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE")
                                .fingerprintHash("sha-256")
                                .fingerprintSetup("active")
                                .iceUfrag("F7EF")
                                .icePwd("Fne9d80TdHR1OQGH/Yevg86O")
                                .build()

                )
        );
    }

    //fingerprint attribute at session level
    @Test
    public void testPostAnswers2() throws Exception {
        mockMvc.perform(
                post("/signaling/answers")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
                        .content("v=0\r\n" +
                                "o=- 7400803269221096182 3 IN IP4 127.0.0.1\r\n" +
                                "s=-\r\n" +
                                "t=0 0\r\n" +
                                "a=group:BUNDLE data-f05ee38194f1d722-d1a850e0b951f1cb audio-mixed-5d3d40a6648d7ec7 video-mixed-76f4ed32e2a32f0a\r\n" +
                                "a=msid-semantic: WMS kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH\r\n" +
                                "a=fingerprint:sha-256 E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02\r\n" +
                                "m=application 29995 DTLS/SCTP 5000\r\n" +
                                "c=IN IP4 93.81.219.123\r\n" +
                                "b=AS:30\r\n" +
                                "a=candidate:2955402076 1 udp 2122260223 192.168.0.179 49187 typ host generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:829414888 1 udp 1686052607 93.81.219.123 29995 typ srflx raddr 192.168.0.179 rport 49187 generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:4272170924 1 tcp 1518280447 192.168.0.179 9 typ host tcptype active generation 0 network-id 1 network-cost 10\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:data-f05ee38194f1d722-d1a850e0b951f1cb\r\n" +
                                "a=sctpmap:5000 webrtc-datachannel 1024\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-mixed-5d3d40a6648d7ec7\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=sendonly\r\n" +
                                "a=msid:kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH 5adaf9ec-d058-4650-b216-a10418e173f5\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n" +
                                "a=ssrc:333445445 cname:SWPtyKkVQTBillVh\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:S1Dd\r\n" +
                                "a=ice-pwd:xXCto3PhAU6vjv5IEJdOeqV4\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-mixed-76f4ed32e2a32f0a\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=sendonly\r\n" +
                                "a=msid:kmT3x3eTkKN3zVrLMC2ErWc4cb0WnqcemCMH 8554d878-e7f1-4c88-b195-d1130bcb476f\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "a=ssrc-group:FID 950630992 950630993\r\n" +
                                "a=ssrc:950630992 cname:SWPtyKkVQTBillVh\r\n" +
                                "a=ssrc:950630993 cname:SWPtyKkVQTBillVh\r\n" +
                                "\r\n" +
                                "v=0\r\n" +
                                "o=- 8445871945891786833 3 IN IP4 127.0.0.1\r\n" +
                                "s=-\r\n" +
                                "t=0 0\r\n" +
                                "a=group:BUNDLE data-b6eef289d220c9c7-5ec9d859fdf20211 audio-fake video-fake video-333ad77887c4a400 audio-ff946d001a80c23\r\n" +
                                "a=msid-semantic: WMS\r\n" +
                                "a=fingerprint:sha-256 65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE\r\n" +
                                "m=application 33429 DTLS/SCTP 5000\r\n" +
                                "c=IN IP4 93.81.219.123\r\n" +
                                "b=AS:30\r\n" +
                                "a=candidate:2955402076 1 udp 2122260223 192.168.0.179 53515 typ host generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:829414888 1 udp 1686052607 93.81.219.123 33429 typ srflx raddr 192.168.0.179 rport 53515 generation 0 network-id 1 network-cost 10\r\n" +
                                "a=candidate:4272170924 1 tcp 1518280447 192.168.0.179 9 typ host tcptype active generation 0 network-id 1 network-cost 10\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:data-b6eef289d220c9c7-5ec9d859fdf20211\r\n" +
                                "a=sctpmap:5000 webrtc-datachannel 1024\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-fake\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=inactive\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-fake\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=inactive\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "m=video 9 RTP/SAVPF 100\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:video-333ad77887c4a400\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=recvonly\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:100 VP8/90000\r\n" +
                                "a=rtcp-fb:100 transport-cc\r\n" +
                                "a=rtcp-fb:100 ccm fir\r\n" +
                                "a=rtcp-fb:100 nack\r\n" +
                                "a=rtcp-fb:100 nack pli\r\n" +
                                "m=audio 9 RTP/SAVPF 111\r\n" +
                                "c=IN IP4 0.0.0.0\r\n" +
                                "a=rtcp:9 IN IP4 0.0.0.0\r\n" +
                                "a=ice-ufrag:F7EF\r\n" +
                                "a=ice-pwd:Fne9d80TdHR1OQGH/Yevg86O\r\n" +
                                "a=ice-options:trickle\r\n" +
                                "a=setup:active\r\n" +
                                "a=mid:audio-ff946d001a80c23\r\n" +
                                "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                                "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                                "a=recvonly\r\n" +
                                "a=rtcp-mux\r\n" +
                                "a=rtpmap:111 opus/48000/2\r\n" +
                                "a=rtcp-fb:111 transport-cc\r\n" +
                                "a=fmtp:111 minptime=10;useinbandfec=1\r\n")
        );

        verify(videobridgeService).processAnswers(
                "test-conference",
                "test-endpoint",
                List.of(
                        Answer.builder()
                                .conferenceId("f05ee38194f1d722")
                                .sctpConnectionId("d1a850e0b951f1cb")
                                .audioChannelId("5d3d40a6648d7ec7")
                                .audioSsrc(List.of(333445445L))
                                .audioSsrcGroups(List.of())
                                .videoChannelId("76f4ed32e2a32f0a")
                                .videoSsrc(List.of(950630992L, 950630993L))
                                .videoSsrcGroups(List.of(Map.entry("FID", List.of(950630992L, 950630993L))))
                                .fingerprintValue("E4:CD:4E:DB:55:C3:96:39:C5:5E:59:62:DC:38:AA:43:5E:43:34:EC:F4:C4:68:D8:F0:A4:94:90:59:DE:1E:02")
                                .fingerprintHash("sha-256")
                                .fingerprintSetup("active")
                                .iceUfrag("S1Dd")
                                .icePwd("xXCto3PhAU6vjv5IEJdOeqV4")
                                .build(),
                        Answer.builder()
                                .conferenceId("b6eef289d220c9c7")
                                .sctpConnectionId("5ec9d859fdf20211")
                                .audioChannelId(null)
                                .audioSsrc(List.of())
                                .audioSsrcGroups(List.of())
                                .videoChannelId(null)
                                .videoSsrc(List.of())
                                .videoSsrcGroups(List.of())
                                .fingerprintValue("65:3E:DD:D9:AA:C1:42:72:00:BF:CC:55:1E:1E:92:FC:1F:98:4A:96:8F:5A:84:31:E4:6A:2E:5A:55:94:00:EE")
                                .fingerprintHash("sha-256")
                                .fingerprintSetup("active")
                                .iceUfrag("F7EF")
                                .icePwd("Fne9d80TdHR1OQGH/Yevg86O")
                                .build()

                )
        );
    }

    @Test
    public void testPostIceCandidate() throws Exception {
        mockMvc.perform(
                post("/signaling/icecandidate")
                        .contentType("application/json")
                        .header("Authorization", "Bearer " + jwt)
                        .content("{\n" +
                                "  \"candidate\": \"candidate:149645548 1 udp 1685987071 192.168.1.2 50697 typ srflx raddr 192.168.8.162 rport 50697 generation 0 ufrag o8rk network-id 1 network-cost 10\",\n" +
                                "  \"sdpMLineIndex\": \"1\",\n" +
                                "  \"sdpMid\": \"data-1-2\"\n" +
                                "}")
        ).andExpect(status().isOk());

        verify(videobridgeService).processIceCandidate(
                "test-conference",
                "test-endpoint",
                "1",
                "149645548",
                1,
                Candidate.Protocol.UDP,
                1685987071,
                "192.168.1.2",
                50697,
                Candidate.Type.SRFLX,
                "192.168.8.162",
                50697,
                "0"
        );
    }

    @Test
    public void testPatchOffers() throws Exception {
        when(videobridgeService.getOffers(
                "test-conference",
                "test-endpoint",
                List.of(
                        PrevOffer.builder()
                                .conferenceId("b72d94919e472c2c")
                                .sessionId(1605218513L)
                                .sessionVersion(1L)
                                .channels(List.of())
                                .build(),
                        PrevOffer.builder()
                                .conferenceId("b72d94919e472c2c")
                                .sessionId(1605218513L)
                                .sessionVersion(1L)
                                .channels(List.of())
                                .build()
                ))
        ).thenReturn(List.of(
                SessionDescription.builder()
                        .version(0)
                        .origin(Origin.builder()
                                .username("-")
                                .sessId(1605218513L)
                                .sessVersion(1L)
                                .nettype("IN")
                                .addrtype("IP4")
                                .address("0.0.0.0")
                                .build())
                        .sessionName(new SessionName("-"))
                        .build(),
                SessionDescription.builder()
                        .version(0)
                        .origin(Origin.builder()
                                .username("-")
                                .sessId(1605218514L)
                                .sessVersion(1L)
                                .nettype("IN")
                                .addrtype("IP4")
                                .address("0.0.0.0")
                                .build())
                        .sessionName(new SessionName("-"))
                        .build()
        ));

        mockMvc.perform(
                patch("/signaling/offers")
                        .contentType("text/plain")
                        .header("Authorization", "Bearer " + jwt)
                        .content(String.join("\r\n", SessionDescriptionTest.expectedSessionDescriptionString, SessionDescriptionTest.expectedSessionDescriptionString))
        ).andExpect(status().isOk())
        .andExpect(content().string("v=0\r\n" +
                "o=- 1605218513 1 IN IP4 0.0.0.0\r\n" +
                "s=-\r\n" +
                "\r\n" +
                "v=0\r\n" +
                "o=- 1605218514 1 IN IP4 0.0.0.0\r\n" +
                "s=-\r\n"));
    }

    @Test
    public void testDelete() throws Exception {
        mockMvc.perform(
                delete("/signaling").header("Authorization", "Bearer " + jwt)
        ).andExpect(status().isOk());

        verify(videobridgeService).delete("test-conference", "test-endpoint");
    }

}
