package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.exception.BadSdpException;
import com.connectclub.jvbuster.exception.ProgramLogicException;
import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.videobridge.data.PrevOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import com.connectclub.jvbuster.videobridge.data.sdp.*;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class SdpUtils {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Channel getPrimaryChannel(Conference conf, String contentName, String endpoint) {
        return conf.getContents().stream()
                .filter(x -> contentName.equals(x.getName()))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel)x)
                .filter(x -> endpoint.equals(x.getEndpoint()))
                .filter(x -> x.getDirection() == Channel.Direction.SENDRECV)
                .findFirst().orElse(null);
    }

    private List<Channel> getAnotherChannels(Conference conf, String contentName, String exceptEndpoint) {
        return conf.getContents().stream()
                .filter(x -> contentName.equals(x.getName()))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel)x)
                .filter(x -> !exceptEndpoint.equalsIgnoreCase(x.getEndpoint()))
                .filter(channel -> channel.getSsrcs() != null && channel.getSsrcs().size() > 0)
                .collect(Collectors.toList());
    }

    public SessionDescription toOfferSdp(
            Conference conference,
            String endpoint,
            Long videoBandwidth,
            PrevOffer prevOffer
    ) {
        if (Stream.ofNullable(conference.getEndpoints())
                .flatMap(Collection::stream)
                .noneMatch(x -> Objects.equals(x.getId(), endpoint))
        ) {
            throw new EndpointNotFound();
        }

        Channel primaryAudioChannel = getPrimaryChannel(conference, "audio", endpoint);
        Channel primaryVideoChannel = getPrimaryChannel(conference, "video", endpoint);

        List<Channel> anotherAudioChannels = getAnotherChannels(conference, "audio", endpoint);
        List<Channel> anotherVideoChannels = getAnotherChannels(conference, "video", endpoint);

        SctpConnection sctpConnection = conference.getContents().stream()
                .filter(x -> "data".equals(x.getName()))
                .map(Content::getSctpConnections)
                .flatMap(Collection::stream)
                .filter(x -> Objects.equals(endpoint, x.getEndpoint()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Can not find sctp connection for endpoint(%s) in conference:\n%s", endpoint, gson.toJson(conference))));

        List<Attribute> candidateAttributes = conference.getChannelBundles().stream()
                .filter(x -> Objects.equals(x.getId(), endpoint))
                .flatMap(x -> x.getTransport().getCandidates().stream()
                        .map(candidate ->
                                CandidateAttribute.builder()
                                        .foundation(candidate.getFoundation())
                                        .componentId(candidate.getComponent())
                                        .transport(gson.toJsonTree(candidate.getProtocol()).getAsString())
                                        .priority(candidate.getPriority())
                                        .address(candidate.getIp())
                                        .port(candidate.getPort())
                                        .type(gson.toJsonTree(candidate.getType()).getAsString())
                                        .relAddr(candidate.getRelAddr())
                                        .relPort(candidate.getRelPort())
                                        .extensions(Collections.singletonList(new CandidateAttribute.Extension("generation", candidate.getGeneration())))
                                        .build()
                        ))
                .collect(Collectors.toList());

        Transport transport = conference.getChannelBundles().stream()
                .filter(x -> Objects.equals(x.getId(), endpoint))
                .map(ChannelBundle::getTransport)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Can not find channel bundle for endpoint(%s) in conference:\n%s", endpoint, gson.toJson(conference))));

        List<Attribute> transportAttributes = List.of(
                new IceUfragAttribute(transport.getUfrag()),
                new IcePasswordAttribute(transport.getPwd()),
                new FingerprintAttribute(transport.getFingerprints().get(0).getHash(), transport.getFingerprints().get(0).getFingerprint()),
                new SetupAttribute(transport.getFingerprints().get(0).getSetup())
        );
        Attribute rtcpMuxAttribute = transport.isRtcpMux() ? new BaseAttribute("rtcp-mux") : NullAttribute.INSTANCE;

        RealTimeControlProtocolAttribute rtcpAttribute = RealTimeControlProtocolAttribute.builder()
                .port(1)
                .netType("IN")
                .addrType("IP4")
                .address("0.0.0.0")
                .build();

        List<Connection> connections = Collections.singletonList(new Connection("IN", "IP4", "0.0.0.0"));

        Function<String, MediaDescription.MediaDescriptionBuilder> audioMediaBuilderSupplier = (direction) ->
                MediaDescription.builder()
                        .media("audio")
                        .port(1)
                        .proto(List.of("RTP", "SAVPF"))
                        .format(111)
                        .connections(connections)
                        .attribute(rtcpAttribute)
                        .attributes(List.of(
                                new BaseAttribute(direction),
                                RTPMapAttribute.builder().format(111).name("opus").rate(48000).parameters("2").build(),
                                FormatAttribute.builder()
                                        .format(111)
                                        .parameters(Map.of(
                                                "minptime", "10",
                                                "useinbandfec", "1",
                                                "stereo", "0"))
                                        .build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(111).type("transport-cc").build(),
                                new BaseAttribute("extmap", "1 urn:ietf:params:rtp-hdrext:ssrc-audio-level"),
                                new BaseAttribute("extmap", "5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                        ));

        Function<String, MediaDescription.MediaDescriptionBuilder> videoMediaBuilderSupplier = (direction) ->
                MediaDescription.builder()
                        .media("video")
                        .port(1)
                        .proto(List.of("RTP", "SAVPF"))
                        .connections(connections)
                        .attribute(rtcpAttribute)
                        .attribute(new BaseAttribute(direction))
                        .format(100)
                        .format(96)
                        .attributes(List.of(
                                RTPMapAttribute.builder().format(100).name("VP8").rate(90000).build(),
                                RTPMapAttribute.builder().format(96).name("rtx").rate(90000).build(),
                                FormatAttribute.builder()
                                        .format(100)
                                        .parameters(Map.of(
                                                "max-fr", "30",
                                                "max-recv-width", "360",
                                                "max-recv-height", "360"
                                        ))
                                        .build(),
                                FormatAttribute.builder()
                                        .format(96)
                                        .parameters(Map.of("apt", "100"))
                                        .build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(100).type("ccm").subtype("fir").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(100).type("nack").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(100).type("nack").subtype("pli").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(100).type("transport-cc").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(96).type("ccm").subtype("fir").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(96).type("nack").build(),
                                RealTimeControlProtocolFeedbackAttribute.builder().format(96).type("nack").subtype("pli").build()
                        ))
                        .attributes(List.of(
                                new BaseAttribute("extmap", "3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time"),
                                new BaseAttribute("extmap", "5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                        ));

        GroupAttribute.GroupAttributeBuilder groupAttributeBuilder = GroupAttribute.builder().semantics("BUNDLE");
        MsidSemanticAttribute.MsidSemanticAttributeBuilder msidSemanticAttributeBuilder = MsidSemanticAttribute.builder().semanticToken(" WMS");

        SessionDescription.SessionDescriptionBuilder sdpBuilder = SessionDescription.builder()
                .version(0)
                .origin(
                        Origin.builder()
                                .username("-")
                                .sessId(prevOffer == null ? LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) : prevOffer.getSessionId())
                                .sessVersion(prevOffer == null ? 1L : prevOffer.getSessionVersion() + 1)
                                .nettype("IN")
                                .addrtype("IP4")
                                .address("0.0.0.0")
                                .build()
                )
                .sessionName(new SessionName("-"))
                .times(Collections.singletonList(new Time(0, 0)))
                .attributes(transportAttributes)
                .media(MediaDescription.builder()
                        .media("application")
                        .port(1)
                        .proto(List.of("DTLS", "SCTP"))
                        .format(5000)
                        .connections(List.of(new Connection("IN", "IP4", "0.0.0.0")))
                        .attributes(List.of(
                                SCTPMapAttribute.builder()
                                        .number(5000)
                                        .app("webrtc-datachannel")
                                        .streams(1024)
                                        .build(),
                                new BaseAttribute("sendrecv"),
                                new MidAttribute(groupAttributeBuilder, "data-" + conference.getId() + "-" + sctpConnection.getId())
                        ))
                        .attribute(rtcpMuxAttribute)
                        .attributes(candidateAttributes)
                        .build());

        if (primaryAudioChannel != null) {
//            List<SSRCGroupAttribute> ssrcGroupAttributes = Stream.ofNullable(primaryAudioChannel.getSsrcGroups())
//                    .flatMap(Collection::stream)
//                    .map(x -> new SSRCGroupAttribute(x.getSemantics(), x.getSources()))
//                    .collect(Collectors.toList());
            List<SSRCAttribute> ssrcAttributes = primaryAudioChannel.getSources().stream()
                    .map(x -> new SSRCAttribute(x, "cname", "mixed"))
                    .collect(Collectors.toList());
            sdpBuilder.media(audioMediaBuilderSupplier.apply("recvonly")
                    .bandwidth(new Bandwidth("AS", 16))
                    .attribute(new MidAttribute(groupAttributeBuilder, "audio-mixed-" + primaryAudioChannel.getId()))
                    .attribute(new MsidAttribute(msidSemanticAttributeBuilder, "mixedmslabel", "audio"))
//                    .attributes(ssrcGroupAttributes)
                    .attributes(ssrcAttributes)
                    .attribute(rtcpMuxAttribute)
                    .attributes(candidateAttributes)
                    .build()
            );
        } else {
            sdpBuilder.media(audioMediaBuilderSupplier.apply("inactive")
                    .attribute(new MidAttribute(groupAttributeBuilder, "audio-fake"))
                    .attribute(new MsidAttribute(msidSemanticAttributeBuilder, "fakemslabel", "audio"))
                    .attribute(rtcpMuxAttribute)
                    .attributes(candidateAttributes)
                    .build());
        }

        if (primaryVideoChannel != null) {
//            List<SSRCGroupAttribute> ssrcGroupAttributes = Stream.ofNullable(primaryVideoChannel.getSsrcGroups())
//                    .flatMap(Collection::stream)
//                    .map(x -> new SSRCGroupAttribute(x.getSemantics(), x.getSources()))
//                    .collect(Collectors.toList());
            List<SSRCAttribute> ssrcAttributes = primaryVideoChannel.getSources().stream()
                    .map(x -> new SSRCAttribute(x, "cname", "mixed"))
                    .collect(Collectors.toList());
            sdpBuilder.media(videoMediaBuilderSupplier.apply("recvonly")
                    .bandwidth(new Bandwidth("AS", videoBandwidth==null ? 200 : videoBandwidth))
                    .attribute(new MidAttribute(groupAttributeBuilder, "video-mixed-" + primaryVideoChannel.getId()))
                    .attribute(new MsidAttribute(msidSemanticAttributeBuilder, "mixedmslabel", "video"))
//                    .attributes(ssrcGroupAttributes)
                    .attributes(ssrcAttributes)
                    .attribute(rtcpMuxAttribute)
                    .attributes(candidateAttributes)
                    .build()
            );
        } else {
            sdpBuilder.media(videoMediaBuilderSupplier.apply("inactive")
                    .attribute(new MidAttribute(groupAttributeBuilder, "video-fake"))
                    .attribute(new MsidAttribute(msidSemanticAttributeBuilder, "fakemslabel", "video"))
                    .attribute(rtcpMuxAttribute)
                    .attributes(candidateAttributes)
                    .build()
            );
        }
        Map<String, MediaDescription> originalMedias = Stream.concat(
                anotherAudioChannels.stream()
                        .map(channel -> Maps.immutableEntry(
                                channel,
                                audioMediaBuilderSupplier.apply("sendonly")
                                        .attribute(new MidAttribute(groupAttributeBuilder, "audio-" + channel.getId()))
                        )),
                anotherVideoChannels.stream()
                        .map(channel -> Maps.immutableEntry(
                                channel,
                                videoMediaBuilderSupplier.apply("sendonly")
                                        .attribute(new MidAttribute(groupAttributeBuilder, "video-" + channel.getId()))
                                )
                        ))
                .peek(x -> x.getValue()
                        .attribute(rtcpMuxAttribute)
                        .attributes(candidateAttributes)
                        .attribute(new MsidAttribute(msidSemanticAttributeBuilder, x.getKey().getEndpoint(), x.getKey().getId())))
                .peek(x -> Stream.ofNullable(x.getKey().getSsrcGroups())
                        .flatMap(Collection::stream)
                        .forEach(ssrcGroup -> x.getValue()
                                .attribute(new SSRCGroupAttribute(ssrcGroup.getSemantics(), ssrcGroup.getSources()))
                        )
                )
                .peek(x -> x.getKey().getSsrcs().forEach(ssrc -> x.getValue().attribute(new SSRCAttribute(ssrc, "cname", x.getKey().getEndpoint()))))
                .collect(Collectors.toMap(
                        x -> x.getKey().getId(),
                        x -> x.getValue().build()
                ));
        List<MediaDescription> medias = new ArrayList<>();
        if (prevOffer != null) {
            for (String channel : prevOffer.getChannels()) {
                if (channel.contains("-")) {
                    String[] t = channel.split("-");
                    String media = t[0];
                    String channelId = t[1];
                    MediaDescription mediaDescription = originalMedias.remove(channelId);
                    if (mediaDescription == null) {
                        mediaDescription = MediaDescription.builder()
                                .media(media)
                                .proto(List.of("RTP", "SAVPF"))
                                .port(0)
                                .format(0)
                                .build();
                    }
                    medias.add(mediaDescription);
                } else {
                    medias.add(
                            MediaDescription.builder()
                                    .media(channel)
                                    .proto(List.of("RTP", "SAVPF"))
                                    .port(-1)
                                    .format(0)
                                    .build()
                    );
                }
            }
        }
        for (int i = 0; i < medias.size(); i++) {
            if (medias.get(i).getPort() == -1) {
                if (originalMedias.size() > 0) {
                    Iterator<Map.Entry<String, MediaDescription>> iterator = originalMedias.entrySet().iterator();
                    medias.set(i, iterator.next().getValue());
                    iterator.remove();
                } else {
                    medias.get(i).setPort(0);
                }
            }
        }
        medias.addAll(originalMedias.values());

//        while (medias.size() < 200) {
//            medias.add(
//                    MediaDescription.builder()
//                            .media("audio")
//                            .proto(List.of("RTP", "SAVPF"))
//                            .port(0)
//                            .format("0")
//                            .build());
//            medias.add(
//                    MediaDescription.builder()
//                            .media("video")
//                            .proto(List.of("RTP", "SAVPF"))
//                            .port(0)
//                            .format("0")
//                            .build());
//        }

        sdpBuilder.medias(medias)
                .attribute(groupAttributeBuilder.build())
                .attribute(msidSemanticAttributeBuilder.build());

        return sdpBuilder.build();
    }

    public String[] splitSdpIntoSections(String sdp) {
        if (!sdp.contains("\r\n")) {
            throw new BadSdpException("SDP must contain correct line delimiter `\\r\\n`");
        }
        String[] sdpSections = sdp.split("\r\n(?=m=)");
        if (sdpSections.length < 2) {
            throw new BadSdpException("SDP must contain at least 1 media section");
        }
        return sdpSections;
    }

    public String[] getMediaDescriptionLines(String[] sdpSections, String mediaType, String condition) {
        return Arrays.stream(sdpSections)
                .filter(x -> x.startsWith("m=" + mediaType))
                .filter(x -> x.contains(condition))
                .findFirst()
                .map(x -> x.split("\r\n"))
                .orElse(new String[0]);
    }

    public Origin getOrigin(String[] sdpSections) {
        StringTokenizer lineTokenizer = new StringTokenizer(sdpSections[0], "\r\n");
        String originAttribute = null;
        while (lineTokenizer.hasMoreTokens()) {
            String line = lineTokenizer.nextToken();
            if (line.startsWith("o=")) {
                originAttribute = line;
                break;
            }
        }
        if (!StringUtils.hasLength(originAttribute)) throw new BadSdpException("no origin attribute");

        String[] originParams = originAttribute.substring(2).split("\\s+");
        if (originParams.length != 6) {
            throw new BadSdpException("Expect 6 parameters in origin(o=) field");
        }
        return Origin.builder()
                .username(originParams[0])
                .sessId(Long.parseLong(originParams[1]))
                .sessVersion(Long.parseLong(originParams[2]))
                .nettype(originParams[3])
                .addrtype(originParams[4])
                .address(originParams[5])
                .build();
    }

    public List<String> getOtherMediaIds(String[] sdpSections) {
        return Arrays.stream(sdpSections)
                .skip(2)//skip first non media section and data- media section
                .map(x -> {
                    StringTokenizer sectionLineTokenizer = new StringTokenizer(x, "\r\n");
                    while (sectionLineTokenizer.hasMoreTokens()) {
                        String token = sectionLineTokenizer.nextToken();
                        if (token.startsWith("m=")) {
                            String[] mediaParams = token.substring("m=".length()).split("\\s+");
                            String media = mediaParams[0];
                            String port = mediaParams[1];
                            if ("0".equals(port)) return media;
                        } else if (token.startsWith("a=mid:")) {
                            return token.substring("a=mid:".length());
                        }
                    }
                    throw new ProgramLogicException();
                })
                .filter(x -> !x.startsWith("audio-mixed-"))
                .filter(x -> !"audio-fake".equals(x))
                .filter(x -> !x.startsWith("video-mixed-"))
                .filter(x -> !"video-fake".equals(x))
                .collect(Collectors.toList());
    }
}
