package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.repository.data.JvbConferenceData;
import com.connectclub.jvbuster.videobridge.data.VideobridgeSsrcGroup;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class JvbConferenceUtils {
    private final List<PayloadType> audioPayloadTypes = List.of(
            PayloadType.builder()
                    .id(111)
                    .name("opus")
                    .clockrate(48000)
                    .channels(2)
                    .parameters(Map.of(
                            "minptime", 10,
                            "useinbandfec", 1
                    ))
                    .rtcpFbs(List.of(
                            RtcpFb.builder().type("transport-cc").build()
                    ))
                    .build()
    );

    private final List<RtpHdrext> audioRtpHdrexts = List.of(
            RtpHdrext.builder()
                    .id(1)
                    .uri("urn:ietf:params:rtp-hdrext:ssrc-audio-level")
                    .build(),
            RtpHdrext.builder()
                    .id(5)
                    .uri("http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                    .build()
    );

    private final List<PayloadType> videoPayloadTypes = List.of(
            PayloadType.builder()
                    .id(100)
                    .name("VP8")
                    .clockrate(90000)
                    .parameters(Map.of(
                            "max-fr", 30,
                            "max-recv-width", 480,
                            "max-recv-height", 320
                    ))
                    .rtcpFbs(List.of(
                            RtcpFb.builder().type("ccm").subtype("fir").build(),
                            RtcpFb.builder().type("nack").build(),
                            RtcpFb.builder().type("nack").subtype("pli").build(),
                            RtcpFb.builder().type("transport-cc").build()
                    ))
                    .build()
    );

    private final List<RtpHdrext> videoRtpHdrexts = List.of(
            RtpHdrext.builder()
                    .id(1)
                    .uri("urn:ietf:params:rtp-hdrext:ssrc-audio-level")
                    .build(),
            RtpHdrext.builder()
                    .id(5)
                    .uri("http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01")
                    .build()
    );

    private Content constructAudioContent(String channelId, String endpoint, Channel.Direction direction, List<Long> sources, List<VideobridgeSsrcGroup> ssrcGroups) {
        Channel channel = Channel.builder()
                .id(channelId)
                .expire(10)
                .initiator(true)
                .endpoint(endpoint)
                .direction(direction)
                .channelBundleId(endpoint)
                .rtpLevelRelayType(Channel.RtpLevelRelayType.TRANSLATOR)
                .lastN(0)
                .payloadTypes(audioPayloadTypes)
                .sources(sources)
                .ssrcGroups(
                        Stream.ofNullable(ssrcGroups)
                                .flatMap(Collection::stream)
                                .map(x -> SsrcGroup.build(x.getSemantics(), x.getSsrcs()))
                                .collect(Collectors.toList())
                )
                .rtpHdrexts(audioRtpHdrexts)
                .build();
        return Content.builder()
                .name("audio")
                .channels(List.of(channel))
                .build();

    }

    private Content constructVideoContent(String channelId, String endpoint, Channel.Direction direction, List<Long> sources, List<VideobridgeSsrcGroup> ssrcGroups) {
        Channel channel = Channel.builder()
                .id(channelId)
                .expire(10)
                .initiator(true)
                .endpoint(endpoint)
                .direction(direction)
                .channelBundleId(endpoint)
                .rtpLevelRelayType(Channel.RtpLevelRelayType.TRANSLATOR)
                .lastN(0)
                .payloadTypes(videoPayloadTypes)
                .sources(sources)
                .ssrcGroups(
                        Stream.ofNullable(ssrcGroups)
                                .flatMap(Collection::stream)
                                .map(x -> SsrcGroup.build(x.getSemantics(), x.getSsrcs()))
                                .collect(Collectors.toList())
                )
                .rtpHdrexts(videoRtpHdrexts)
                .build();
        return Content.builder()
                .name("video")
                .channels(List.of(channel))
                .build();
    }

    private Content constructDataContent(String sctpConnectionId, String endpoint) {
        SctpConnection sctpConnection = SctpConnection.builder()
                .id(sctpConnectionId)
                .expire(10)
                .initiator(true)
                .endpoint(endpoint)
                .port(5000)
                .channelBundleId(endpoint)
                .build();
        return Content.builder()
                .name("data")
                .sctpConnections(List.of(sctpConnection))
                .build();
    }

    public Conference buildPatchWithNewEndpoint(JvbConferenceData jvbConferenceData, String endpoint, String displayName, Channel.Direction channelsDirection) {
        return Conference.builder()
                .id(jvbConferenceData.getConfId())
                .gid(jvbConferenceData.getGid())
                .contents(List.of(
                        constructAudioContent(null, endpoint, channelsDirection, null, null),
                        constructVideoContent(null, endpoint, channelsDirection, null, null),
                        constructDataContent(null, endpoint)
                ))
                .endpoints(List.of(Endpoint.builder()
                        .id(endpoint)
                        .displayName(displayName)
                        .build()))
                .channelBundles(List.of(
                        ChannelBundle.builder()
                                .id(endpoint)
                                .transport(
                                        Transport.builder()
                                                .xmlns("urn:xmpp:jingle:transports:ice-udp:1")
                                                .rtcpMux(true)
                                                .build()
                                )
                                .build()
                ))
                .build();
    }

    public Conference buildPatchBasedOnAnswer(String conferenceGid, String endpoint, VideobridgeConferenceAnswer answer) {
        List<Content> contents = new ArrayList<>();
        contents.add(constructDataContent(answer.getSctpConnectionId(), endpoint));
        if (answer.getPrimaryAudioChannel() != null) {
            contents.add(constructAudioContent(
                    answer.getPrimaryAudioChannel().getId(),
                    endpoint,
                    Channel.Direction.SENDRECV,
                    answer.getPrimaryAudioChannel().getSsrcs(),
                    answer.getPrimaryAudioChannel().getSsrcGroups()
            ));
        }

        if (answer.getPrimaryVideoChannel() != null) {
            contents.add(constructVideoContent(
                    answer.getPrimaryVideoChannel().getId(),
                    endpoint,
                    Channel.Direction.SENDRECV,
                    answer.getPrimaryVideoChannel().getSsrcs(),
                    answer.getPrimaryVideoChannel().getSsrcGroups()
            ));
        }

        return Conference.builder()
                .id(answer.getConferenceId())
                .gid(conferenceGid)
                .contents(contents)
                .channelBundles(List.of(
                        ChannelBundle.builder()
                                .id(endpoint)
                                .transport(
                                        Transport.builder()
                                                .xmlns("urn:xmpp:jingle:transports:ice-udp:1")
                                                .rtcpMux(true)
                                                .ufrag(answer.getUfrag())
                                                .pwd(answer.getPwd())
                                                .fingerprints(
                                                        answer.getFingerprints().stream()
                                                                .map(x -> Fingerprint.builder()
                                                                        .fingerprint(x.getValue())
                                                                        .hash(x.getHash())
                                                                        .setup(x.getSetup())
                                                                        .build())
                                                                .collect(Collectors.toList())
                                                )
                                                .build()
                                )
                                .build()
                ))
                .build();
    }

    private List<String> getChannelsIds(Conference conference, String contentName, String endpoint) {
        if (conference.getContents() == null) return List.of();
        return conference.getContents().stream()
                .filter(x -> x.getName().equals(contentName))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel) x)
                .filter(x -> x.getEndpoint().equals(endpoint))
                .map(Channel::getId)
                .collect(Collectors.toList());
    }

    private List<String> getSctpConnectionsIds(Conference conference, String endpoint) {
        if (conference.getContents() == null) return List.of();
        return conference.getContents().stream()
                .filter(x -> "data".equals(x.getName()))
                .map(Content::getSctpConnections)
                .flatMap(Collection::stream)
                .filter(x -> x.getEndpoint().equals(endpoint))
                .map(SctpConnection::getId)
                .collect(Collectors.toList());
    }

    public Conference buildPatchWithExpiredEndpoint(Conference conference, String endpoint) {
        List<String> oldAudioChannelsIds = getChannelsIds(conference, "audio", endpoint);
        List<String> oldVideoChannelsIds = getChannelsIds(conference, "video", endpoint);
        List<String> oldSctpConnectionId = getSctpConnectionsIds(conference, endpoint);

        if (oldAudioChannelsIds.size() == 0 && oldVideoChannelsIds.size() == 0 && oldSctpConnectionId.size() == 0) {
            return null;
        }

        return Conference.builder()
                .id(conference.getId())
                .gid(conference.getGid())
                .contents(List.of(
                        Content.builder()
                                .name("audio")
                                .channels(
                                        oldAudioChannelsIds.stream()
                                                .map(x -> Channel.builder()
                                                        .id(x)
                                                        .expire(0)
                                                        .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build(),
                        Content.builder()
                                .name("video")
                                .channels(
                                        oldVideoChannelsIds.stream()
                                                .map(x -> Channel.builder()
                                                        .id(x)
                                                        .expire(0)
                                                        .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build(),
                        Content.builder()
                                .name("data")
                                .sctpConnections(
                                        oldSctpConnectionId.stream()
                                                .map(x -> SctpConnection.builder()
                                                        .id(x)
                                                        .expire(0)
                                                        .build())
                                                .collect(Collectors.toList())
                                )
                                .build()
                ))
                .build();
    }

    public Conference buildPatchWithOcto(
            String gid,
            String confId,
            Set<String> relays,
            Map<String, UUID> endpoints,
            List<Map.Entry<String, List<Long>>> audioSources,
            List<Map.Entry<String, List<Long>>> videoSources,
            List<Map.Entry<String, List<Long>>> videoSourceGroups
    ) {
        return Conference.builder()
                .id(confId)
                .gid(gid)
                .contents(List.of(
                        Content.builder()
                                .name("audio")
                                .channel(
                                        OctoChannel.builder()
                                                .id("octo-audio")
                                                .expire(1)
                                                .payloadTypes(audioPayloadTypes)
                                                .rtpHdrexts(audioRtpHdrexts)
                                                .sources(audioSources.stream()
                                                        .flatMap(x -> x.getValue().stream().map(y -> Map.entry(x.getKey(), y)))
                                                        .map(x -> Source.builder()
                                                                .endpointId(x.getKey())
                                                                .endpointUuid(endpoints.get(x.getKey()))
                                                                .ssrc(x.getValue())
                                                                .build()
                                                        )
                                                        .collect(Collectors.toList())
                                                )
                                                .relays(relays)
                                                .build()
                                )
                                .build(),
                        Content.builder()
                                .name("video")
                                .channel(
                                        OctoChannel.builder()
                                                .id("octo-video")
                                                .expire(1)
                                                .payloadTypes(videoPayloadTypes)
                                                .rtpHdrexts(videoRtpHdrexts)
                                                .sources(videoSources.stream()
                                                        .flatMap(x -> x.getValue().stream().map(y -> Map.entry(x.getKey(), y)))
                                                        .map(x -> Source.builder()
                                                                .endpointId(x.getKey())
                                                                .endpointUuid(endpoints.get(x.getKey()))
                                                                .ssrc(x.getValue())
                                                                .build()
                                                        )
                                                        .collect(Collectors.toList())
                                                )
                                                .ssrcGroups(videoSourceGroups.stream()
                                                        .map(x -> SsrcGroup.build(x.getKey(), x.getValue()))
                                                        .collect(Collectors.toList())
                                                )
                                                .relays(relays)
                                                .build()
                                )
                                .build()
                ))
                .build();
    }

    public Conference buildPatchWithAudioMixer(
            String gid,
            String confId
    ) {
        return Conference.builder()
                .id(confId)
                .gid(gid)
                .contents(List.of(
                        Content.builder()
                                .name("audio")
                                .channel(
                                        Channel.builder()
                                                .id("audio-mixer")
                                                .expire(1)
                                                .payloadTypes(audioPayloadTypes)
                                                .rtpLevelRelayType(Channel.RtpLevelRelayType.MIXER)
                                                .build())
                                .build()
                ))
                .build();
    }
}
