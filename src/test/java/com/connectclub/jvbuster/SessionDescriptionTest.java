package com.connectclub.jvbuster;

import com.connectclub.jvbuster.videobridge.data.sdp.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SessionDescriptionTest {

    public final static SessionDescription sessionDescription = SessionDescription.builder()
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
            .attributes(List.of(
                    GroupAttribute.builder()
                            .semantics("BUNDLE")
                            .tags(List.of("data-b72d94919e472c2c-48a8973ae7db7b72", "audio-mixed-ce77ff3c6662a772", "video-mixed-ef362b0854d6be78"))
                            .build(),
                    MsidSemanticAttribute.builder().semanticToken("WMS")
                            .stream("mixedmslabel")
                            .build()
            ))
            .times(List.of(new Time(0, 0)))
            .medias(List.of(
                    MediaDescription.builder()
                            .media("application")
                            .port(1)
                            .proto(List.of("DTLS", "SCTP"))
                            .format(5000)
                            .connections(List.of(Connection.builder()
                                    .netType("IN")
                                    .addrType("IP4")
                                    .address("0.0.0.0")
                                    .build()))
                            .attributes(List.of(
                                    SCTPMapAttribute.builder()
                                            .number(5000)
                                            .app("webrtc-datachannel")
                                            .streams(1024)
                                            .build(),
                                    new BaseAttribute("sendrecv"),
                                    new MidAttribute("data-b72d94919e472c2c-48a8973ae7db7b72"),
                                    new IceUfragAttribute("bdh491emupttk0"),
                                    new IcePasswordAttribute("614lli8mnlkn8omu9uf0crh1ja"),
                                    FingerprintAttribute.builder()
                                            .hashFunc("sha-256")
                                            .fingerprint("4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B")
                                            .build(),
                                    new SetupAttribute("actpass"),
                                    new BaseAttribute("rtcp-mux"),
                                    CandidateAttribute.builder()
                                            .foundation("1")
                                            .componentId(1)
                                            .transport("udp")
                                            .priority(2130706431)
                                            .address("192.168.0.179")
                                            .port(56228)
                                            .type("host")
                                            .extensions(List.of(new CandidateAttribute.Extension("generation", "0")))
                                            .build()
                            ))
                            .build(),
                    MediaDescription.builder()
                            .media("audio")
                            .port(1)
                            .proto(List.of("RTP", "SAVPF"))
                            .format(111)
                            .connections(List.of(Connection.builder()
                                    .netType("IN")
                                    .addrType("IP4")
                                    .address("0.0.0.0")
                                    .build()))
                            .bandwidth(new Bandwidth("AS", 16))
                            .attributes(List.of(
                                    RealTimeControlProtocolAttribute.builder()
                                            .port(1)
                                            .netType("IN")
                                            .addrType("IP4")
                                            .address("0.0.0.0")
                                            .build(),
                                    new BaseAttribute("recvonly"),
                                    RTPMapAttribute.builder()
                                            .format(111)
                                            .name("opus")
                                            .rate(48000)
                                            .parameters("2")
                                            .build(),
                                    FormatAttribute.builder()
                                            .format(111)
                                            .parameters(Map.of(
                                                    "useinbandfec", "1",
                                                    "minptime", "10"))
                                            .build(),
                                    new BaseAttribute("rtcp-fb", "111 transport-cc"),
                                    new BaseAttribute("extmap", "1 urn:ietf:params:rtp-hdrext:ssrc-audio-level"),
                                    new BaseAttribute("extmap", "5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01"),
                                    new MidAttribute("audio-mixed-ce77ff3c6662a772"),
                                    new MsidAttribute("mixedmslabel", "audio"),
                                    SSRCAttribute.builder()
                                            .SSRC(160917409L)
                                            .attrField("cname")
                                            .attrValue("mixed")
                                            .build(),
                                    new IceUfragAttribute("bdh491emupttk0"),
                                    new IcePasswordAttribute("614lli8mnlkn8omu9uf0crh1ja"),
                                    FingerprintAttribute.builder()
                                            .hashFunc("sha-256")
                                            .fingerprint("4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B")
                                            .build(),
                                    new SetupAttribute("actpass"),
                                    new BaseAttribute("rtcp-mux"),
                                    CandidateAttribute.builder()
                                            .foundation("1")
                                            .componentId(1)
                                            .transport("udp")
                                            .priority(2130706431)
                                            .address("192.168.0.179")
                                            .port(56228)
                                            .type("host")
                                            .extensions(List.of(new CandidateAttribute.Extension("generation", "0")))
                                            .build()
                            ))
                            .build(),
                    MediaDescription.builder()
                            .media("video")
                            .port(1)
                            .proto(List.of("RTP", "SAVPF"))
                            .format(100)
                            .connections(List.of(Connection.builder()
                                    .netType("IN")
                                    .addrType("IP4")
                                    .address("0.0.0.0")
                                    .build()))
                            .bandwidth(new Bandwidth("AS", 200))
                            .attributes(List.of(
                                    RealTimeControlProtocolAttribute.builder()
                                            .port(1)
                                            .netType("IN")
                                            .addrType("IP4")
                                            .address("0.0.0.0")
                                            .build(),
                                    new BaseAttribute("recvonly"),
                                    RTPMapAttribute.builder()
                                            .format(100)
                                            .name("VP8")
                                            .rate(90000)
                                            .build(),
                                    FormatAttribute.builder()
                                            .format(100)
                                            .parameters(Map.of(
                                                    "max-recv-width", "480",
                                                    "max-fr", "30",
                                                    "max-recv-height", "360"
                                            ))
                                            .build(),
                                    new BaseAttribute("rtcp-fb", "100 ccm fir"),
                                    new BaseAttribute("rtcp-fb", "100 nack"),
                                    new BaseAttribute("rtcp-fb", "100 nack pli"),
                                    new BaseAttribute("rtcp-fb", "100 transport-cc"),
                                    new BaseAttribute("extmap", "1 urn:ietf:params:rtp-hdrext:ssrc-audio-level"),
                                    new BaseAttribute("extmap", "5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01"),
                                    new MidAttribute("video-mixed-ef362b0854d6be78"),
                                    new MsidAttribute("mixedmslabel", "video"),
                                    SSRCAttribute.builder()
                                            .SSRC(1686921376L)
                                            .attrField("cname")
                                            .attrValue("mixed")
                                            .build(),
                                    new IceUfragAttribute("bdh491emupttk0"),
                                    new IcePasswordAttribute("614lli8mnlkn8omu9uf0crh1ja"),
                                    FingerprintAttribute.builder()
                                            .hashFunc("sha-256")
                                            .fingerprint("4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B")
                                            .build(),
                                    new SetupAttribute("actpass"),
                                    new BaseAttribute("rtcp-mux"),
                                    CandidateAttribute.builder()
                                            .foundation("1")
                                            .componentId(1)
                                            .transport("udp")
                                            .priority(2130706431)
                                            .address("192.168.0.179")
                                            .port(56228)
                                            .type("host")
                                            .extensions(List.of(new CandidateAttribute.Extension("generation", "0")))
                                            .build()
                            ))
                            .build()
            ))
            .build();

    public final static String expectedSessionDescriptionString =
            "v=0\r\n" +
                    "o=- 1605218513 1 IN IP4 0.0.0.0\r\n" +
                    "s=-\r\n" +
                    "t=0 0\r\n" +
                    "a=group:BUNDLE data-b72d94919e472c2c-48a8973ae7db7b72 audio-mixed-ce77ff3c6662a772 video-mixed-ef362b0854d6be78\r\n" +
                    "a=msid-semantic:WMS mixedmslabel\r\n" +
                    "m=application 1 DTLS/SCTP 5000\r\n" +
                    "c=IN IP4 0.0.0.0\r\n" +
                    "a=sctpmap:5000 webrtc-datachannel 1024\r\n" +
                    "a=sendrecv\r\n" +
                    "a=mid:data-b72d94919e472c2c-48a8973ae7db7b72\r\n" +
                    "a=ice-ufrag:bdh491emupttk0\r\n" +
                    "a=ice-pwd:614lli8mnlkn8omu9uf0crh1ja\r\n" +
                    "a=fingerprint:sha-256 4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B\r\n" +
                    "a=setup:actpass\r\n" +
                    "a=rtcp-mux\r\n" +
                    "a=candidate:1 1 udp 2130706431 192.168.0.179 56228 typ host generation 0\r\n" +
                    "m=audio 1 RTP/SAVPF 111\r\n" +
                    "c=IN IP4 0.0.0.0\r\n" +
                    "b=AS:16\r\n" +
                    "a=rtcp:1 IN IP4 0.0.0.0\r\n" +
                    "a=recvonly\r\n" +
                    "a=rtpmap:111 opus/48000/2\r\n" +
                    "a=fmtp:111 minptime=10; useinbandfec=1\r\n" +
                    "a=rtcp-fb:111 transport-cc\r\n" +
                    "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                    "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                    "a=mid:audio-mixed-ce77ff3c6662a772\r\n" +
                    "a=msid:mixedmslabel audio\r\n" +
                    "a=ssrc:160917409 cname:mixed\r\n" +
                    "a=ice-ufrag:bdh491emupttk0\r\n" +
                    "a=ice-pwd:614lli8mnlkn8omu9uf0crh1ja\r\n" +
                    "a=fingerprint:sha-256 4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B\r\n" +
                    "a=setup:actpass\r\n" +
                    "a=rtcp-mux\r\n" +
                    "a=candidate:1 1 udp 2130706431 192.168.0.179 56228 typ host generation 0\r\n" +
                    "m=video 1 RTP/SAVPF 100\r\n" +
                    "c=IN IP4 0.0.0.0\r\n" +
                    "b=AS:200\r\n" +
                    "a=rtcp:1 IN IP4 0.0.0.0\r\n" +
                    "a=recvonly\r\n" +
                    "a=rtpmap:100 VP8/90000\r\n" +
                    "a=fmtp:100 max-fr=30; max-recv-height=360; max-recv-width=480\r\n" +
                    "a=rtcp-fb:100 ccm fir\r\n" +
                    "a=rtcp-fb:100 nack\r\n" +
                    "a=rtcp-fb:100 nack pli\r\n" +
                    "a=rtcp-fb:100 transport-cc\r\n" +
                    "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" +
                    "a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
                    "a=mid:video-mixed-ef362b0854d6be78\r\n" +
                    "a=msid:mixedmslabel video\r\n" +
                    "a=ssrc:1686921376 cname:mixed\r\n" +
                    "a=ice-ufrag:bdh491emupttk0\r\n" +
                    "a=ice-pwd:614lli8mnlkn8omu9uf0crh1ja\r\n" +
                    "a=fingerprint:sha-256 4C:3D:A0:17:D6:0F:B2:33:3E:5B:DF:CD:AA:B4:06:27:47:0A:46:3C:35:9C:30:FB:7A:CF:2E:16:6F:2A:7D:1B\r\n" +
                    "a=setup:actpass\r\n" +
                    "a=rtcp-mux\r\n" +
                    "a=candidate:1 1 udp 2130706431 192.168.0.179 56228 typ host generation 0\r\n";

    @Test
    public void toStringTest() {
        String actualSessionDescriptionString = sessionDescription.append(new StringBuilder()).toString();
        assertEquals(expectedSessionDescriptionString, actualSessionDescriptionString);
    }

}
