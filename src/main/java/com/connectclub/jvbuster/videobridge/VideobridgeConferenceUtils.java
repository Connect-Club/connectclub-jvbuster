package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.videobridge.data.*;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class VideobridgeConferenceUtils {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Channel getPrimaryChannel(Conference conf, String contentName, String endpoint) {
        return conf.getContents().stream()
                .filter(x -> contentName.equals(x.getName()))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel) x)
                .filter(x -> endpoint.equals(x.getEndpoint()))
                .findFirst().orElse(null);
    }

    private List<Channel> getNonPrimaryChannels(Conference conf, String contentName, String exceptEndpoint) {
        return conf.getContents().stream()
                .filter(x -> contentName.equals(x.getName()))
                .map(Content::getChannels)
                .flatMap(Collection::stream)
                .filter(x -> x instanceof Channel)
                .map(x -> (Channel) x)
                .filter(x -> !exceptEndpoint.equalsIgnoreCase(x.getEndpoint()))
                .filter(channel -> channel.getDirection() == Channel.Direction.SENDRECV || channel.getDirection() == Channel.Direction.RECVONLY)
                .filter(channel -> channel.getSsrcs() != null && channel.getSsrcs().size() > 0)
                .collect(Collectors.toList());
    }

    public VideobridgeConferenceOffer toVideobridgeConference(String videobridgeId, Conference conference, String endpoint) {
        if (conference.getEndpoints().stream().noneMatch(x -> Objects.equals(x.getId(), endpoint))) {
            throw new EndpointNotFound();
        }

        SctpConnection sctpConnection = conference.getContents().stream()
                .filter(x -> "data".equals(x.getName()))
                .map(Content::getSctpConnections)
                .flatMap(Collection::stream)
                .filter(x -> Objects.equals(endpoint, x.getEndpoint()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Can not find sctp connection for endpoint(%s) in conference:\n%s", endpoint, gson.toJson(conference))));

        Channel primaryAudioChannel = getPrimaryChannel(conference, "audio", endpoint);
        Channel primaryVideoChannel = getPrimaryChannel(conference, "video", endpoint);

        List<Channel> anotherAudioChannels = getNonPrimaryChannels(conference, "audio", endpoint);
        List<Channel> anotherVideoChannels = getNonPrimaryChannels(conference, "video", endpoint);

        Transport transport = conference.getChannelBundles().stream()
                .filter(x -> Objects.equals(x.getId(), endpoint))
                .map(ChannelBundle::getTransport)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Can not find channel bundle for endpoint(%s) in conference:\n%s", endpoint, gson.toJson(conference))));

        return VideobridgeConferenceOffer.builder()
                .videobridgeId(videobridgeId)
                .conferenceId(conference.getId())
                .ufrag(transport.getUfrag())
                .pwd(transport.getPwd())
                .fingerprints(transport.getFingerprints().stream()
                        .map(x -> VideobridgeFingerprint.builder()
                                .hash(x.getHash())
                                .value(x.getFingerprint())
                                .setup(x.getSetup())
                                .build())
                        .collect(Collectors.toList())
                )
                .candidates(transport.getCandidates().stream()
                        .map(x -> VideobridgeCandidate.builder()
                                .foundation(x.getFoundation())
                                .component(x.getComponent())
                                .protocol(x.getProtocol().name())
                                .priority(x.getPriority())
                                .ip(x.getIp())
                                .port(x.getPort())
                                .type(x.getType().name())
                                .relAddr(x.getRelAddr())
                                .relPort(x.getRelPort())
                                .generation(x.getGeneration())
                                .build())
                        .collect(Collectors.toList())
                )
                .rtcpMux(transport.isRtcpMux())
                .sctpConnectionId(sctpConnection.getId())
                .primaryAudioChannel(
                        VideobridgePrimaryChannel.builder()
                                .id(primaryAudioChannel.getId())
                                .direction(primaryAudioChannel.getDirection().name().toLowerCase())
                                .sources(primaryAudioChannel.getSources())
                                .build()
                )
                .primaryVideoChannel(
                        VideobridgePrimaryChannel.builder()
                                .id(primaryVideoChannel.getId())
                                .direction(primaryVideoChannel.getDirection().name().toLowerCase())
                                .sources(primaryVideoChannel.getSources())
                                .build()
                )
                .audioChannels(anotherAudioChannels.stream()
                        .map(x -> VideobridgeChannel.builder()
                                .id(x.getId())
                                .endpoint(x.getEndpoint())
                                .ssrcs(x.getSsrcs())
                                .ssrcGroups(Stream.ofNullable(x.getSsrcGroups()).flatMap(Collection::stream)
                                        .map(g -> new VideobridgeSsrcGroup(g.getSemantics(), g.getSources()))
                                        .collect(Collectors.toList())
                                )
                                .build())
                        .collect(Collectors.toList())
                )
                .videoChannels(anotherVideoChannels.stream()
                        .map(VideobridgeConferenceUtils::convertToVideobridgeChannel)
                        .collect(Collectors.toList())
                )
                .endpoints(conference.getEndpoints().stream()
                        .map(x -> VideobridgeEndpoint.builder()
                                .id(x.getId())
                                .uuid(x.getUuid())
                                .build())
                        .collect(Collectors.toList())
                )
                .build();
    }

    private static VideobridgeChannel convertToVideobridgeChannel(Channel ch) {
        VideobridgeChannel.VideobridgeChannelBuilder builder = VideobridgeChannel.builder()
                .id(ch.getId())
                .endpoint(ch.getEndpoint());
        List<Long> ssrcs = new ArrayList<>(ch.getSsrcs());
        List<VideobridgeSsrcGroup> ssrcGroups = new ArrayList<>();
        if(ch.getSsrcGroups() != null) {
            Optional<SsrcGroup> simSsrcGroup = ch.getSsrcGroups().stream().filter(x -> "SIM".equals(x.getSemantics())).findFirst();
            if(simSsrcGroup.isPresent()) {
                Set<Long> ssrcsToRemove = simSsrcGroup.get().getSources().stream().skip(1).collect(Collectors.toSet());
                for(SsrcGroup ssrcGroup : ch.getSsrcGroups()) {
                    if("SIM".equals(ssrcGroup.getSemantics())) {
                        continue;
                    }
                    if(ssrcsToRemove.contains(ssrcGroup.getSources().get(0))) {
                        ssrcsToRemove.addAll(ssrcGroup.getSources());
                        continue;
                    }
                    ssrcGroups.add(new VideobridgeSsrcGroup(ssrcGroup.getSemantics(), ssrcGroup.getSources()));
                }
                ssrcs.removeAll(ssrcsToRemove);
            } else {
                builder.ssrcGroups(ch.getSsrcGroups().stream()
                        .map(g -> new VideobridgeSsrcGroup(g.getSemantics(), g.getSources()))
                        .collect(Collectors.toList())
                );
            }
        }
        return builder.ssrcs(ssrcs).ssrcGroups(ssrcGroups).build();
    }

    private static void copyChannels(Conference source, Conference target, String contentName, Set<String> endpointsToCopy) {
        if(source.getContents() == null) return;
        Content content = target.getContents().stream()
                .filter(x -> Objects.equals(contentName, x.getName()))
                .findFirst().orElseThrow();
        if(content.getChannels().size() == 0) {
            String otherContents = target.getContents().stream()
                    .filter(x -> !Objects.equals(contentName, x.getName()))
                    .map(x -> String.format("%s(channels=[%s])", x.getName(), x.getChannels().stream()
                            .map(ChannelCommon::getId)
                            .distinct()
                            .collect(Collectors.joining(", "))))
                    .collect(Collectors.joining(", "));
            log.warn("No channels in content(name={}). Other contents [{}]. Conference(id={})", contentName, otherContents, target.getId());
        }
        List<ChannelCommon> channels = new ArrayList<>(content.getChannels());
        channels.addAll(source.getContents().stream()
                        .filter(x -> contentName.equals(x.getName()))
                        .flatMap(x -> x.getChannels().stream())
                        .filter(x -> !"octo".equals(x.getId()))
                        .map(Channel.class::cast)
                        .filter(x -> endpointsToCopy.contains(x.getEndpoint()))
                        .collect(Collectors.toList())
                );
        content.setChannels(channels);
    }

    public VideobridgeConferenceOffer toVideobridgeConference(String videobridgeId, Conference confForListener, List<Conference> confForSpeakers, String endpoint) {
        Set<UUID> listenerEndpoints = confForListener.getEndpoints().stream()
                .map(Endpoint::getUuid)
                .collect(Collectors.toSet());
        for (Conference confForSpeaker : confForSpeakers) {
            Set<String> primaryEndpoints = confForSpeaker.getEndpoints().stream()
                    .filter(x -> "primary".equals(x.getDisplayName()))
                    .filter(x -> listenerEndpoints.contains(x.getUuid()))
                    .map(Endpoint::getId)
                    .collect(Collectors.toSet());
            copyChannels(confForSpeaker, confForListener, "audio", primaryEndpoints);
            copyChannels(confForSpeaker, confForListener, "video", primaryEndpoints);
        }
        return toVideobridgeConference(videobridgeId, confForListener, endpoint);
    }

}
