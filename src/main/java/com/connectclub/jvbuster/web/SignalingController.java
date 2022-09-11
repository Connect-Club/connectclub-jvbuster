package com.connectclub.jvbuster.web;

import com.connectclub.jvbuster.exception.BadSdpException;
import com.connectclub.jvbuster.exception.ConflictException;
import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.exception.ProgramLogicException;
import com.connectclub.jvbuster.security.VideobridgeAuthenticationToken;
import com.connectclub.jvbuster.utils.MethodArgumentsLogger;
import com.connectclub.jvbuster.videobridge.SdpUtils;
import com.connectclub.jvbuster.videobridge.data.Answer;
import com.connectclub.jvbuster.videobridge.data.PrevOffer;
import com.connectclub.jvbuster.videobridge.data.jvb.Candidate;
import com.connectclub.jvbuster.videobridge.data.sdp.Origin;
import com.connectclub.jvbuster.videobridge.data.sdp.SessionDescription;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.connectclub.jvbuster.videobridge.i.VideobridgeService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Controller
@RequestMapping("/signaling")
public class SignalingController {

    private final static Pattern ssrcPattern = Pattern.compile("^a=ssrc:(?<ssrcId>\\d+)\\s+.+$");
    private final static Pattern ssrcGroupPattern = Pattern.compile("^a=ssrc-group:(?<semantic>\\w+)\\s+(?<ssrcIdList>[0-9 ]+)$");
    private final static Pattern midAttributePattern = Pattern.compile("^a=mid:(?<channelId>.+)$");
    private final static Pattern midDataAttributeDeprecatedPattern = Pattern.compile("^a=mid:data-(?<conferenceId>.+)-(?<sctpConnectionId>.+)$");
    private final static Pattern midDataAttributePattern = Pattern.compile("^a=mid:(?<sctpConnectionId>.+)$");
    private final static Pattern midConfIdAttributePattern = Pattern.compile("^a=mid:confId-(?<conferenceId>.+)$");
    private final static Pattern candidatePattern = Pattern.compile("^a=candidate:(?<foundation>\\S+) (?<componentId>\\d+) (?<transport>\\S+) (?<priority>\\d+) (?<connectionAddress>\\S+) (?<port>\\d+) typ (?<candidateType>\\S+)( raddr (?<relAddress>\\S+))?( rport (?<relPort>\\d+))?( generation (?<generation>\\d+))?.*$");

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final RedissonClient redissonClient;
    private final VideobridgeService videobridgeService;

    public SignalingController(
            RedissonClient redissonClient,
            VideobridgeService videobridgeService
    ) {
        this.redissonClient = redissonClient;
        this.videobridgeService = videobridgeService;
    }

    private String serializeOffers(List<SessionDescription> offers) {
        int mediasCount = offers.stream().mapToInt(x -> x.getMedias().size()).sum();
        int expectedContentLength = 705/*length of media section*/ * mediasCount + 100/*length of header*/; //An empirically derived formula
        StringBuilder stringBuilder = new StringBuilder(expectedContentLength);
        boolean first = true;
        for (SessionDescription offer : offers) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append("\r\n");
            }
            offer.append(stringBuilder);
            log.trace("SDP offer details:\n{}", offer.toDetailsOnlyString());
        }
        if (stringBuilder.length() > expectedContentLength) {
            log.warn("Expected offers string length({}) is less than actual({}).", expectedContentLength, stringBuilder.length());
        }
        return stringBuilder.toString();
    }

    @GetMapping(value = "/offers", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @MethodArgumentsLogger
    public String getSdpOffers(
            VideobridgeAuthenticationToken authToken,
            @RequestParam(value = "videoBandwidth", required = false, defaultValue = "200") Long videoBandwidth
    ) throws IOException, JvbInstanceRestException {
        RLock lock = redissonClient.getFairLock("getOffers-" + authToken.getConferenceGid() + "-" + authToken.getEndpoint());
        if (lock.tryLock()) {
            try {
                return serializeOffers(videobridgeService.getOffers(authToken.getConferenceGid(), authToken.getEndpoint(), videoBandwidth));
            } finally {
                lock.unlock();
            }
        } else {
            throw new ConflictException("Multiple GET offers from the same endpoint is not allowed");
        }
    }

    @GetMapping(value = "/offer/{videobridgeId}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @MethodArgumentsLogger
    public String getSdpOfferForVideobridge(
            @PathVariable("videobridgeId") String videobridgeId,
            @RequestParam(value = "videoBandwidth", required = false, defaultValue = "200") Long videoBandwidth,
            VideobridgeAuthenticationToken authToken
    ) throws IOException, JvbInstanceRestException {
        return serializeOffers(videobridgeService.getOffers(authToken.getConferenceGid(), authToken.getEndpoint(), videoBandwidth, videobridgeId));
    }

    @PostMapping(value = "/answers", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    @MethodArgumentsLogger
    public void processSdpAnswers(
            @RequestBody String sdpAnswers,
            VideobridgeAuthenticationToken authToken
    ) throws IOException, JvbInstanceRestException {
        List<Answer> answers = new ArrayList<>();
        for (String sdpAnswer : sdpAnswers.split("\r\n(?=v=)")) {
            String[] sdpSections = SdpUtils.splitSdpIntoSections(sdpAnswer);

            String[] audioMediaLines = SdpUtils.getMediaDescriptionLines(sdpSections, "audio", "\r\na=sendrecv");
            String audioChannelId = Arrays.stream(audioMediaLines)
                    .map(midAttributePattern::matcher)
                    .filter(Matcher::matches)
                    .map(x -> x.group("channelId"))
                    .findFirst()
                    .map(x -> x.replaceAll("^audio-mixed-", ""))
                    .orElse(null);
            List<Long> audioSsrcList = Arrays.stream(audioMediaLines)
                    .map(ssrcPattern::matcher)
                    .filter(Matcher::matches)
                    .map(x -> x.group("ssrcId"))
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toList());
            List<Map.Entry<String, List<Long>>> audioSsrcGroupList = Arrays.stream(audioMediaLines)
                    .map(ssrcGroupPattern::matcher)
                    .filter(Matcher::matches)
                    .map(x-> Map.entry(x.group("semantic"), Arrays.stream(x.group("ssrcIdList").split("\\s+")).map(Long::parseLong).collect(Collectors.toList())))
                    .collect(Collectors.toList());

            String[] videoMediaLines = SdpUtils.getMediaDescriptionLines(sdpSections, "video", "\r\na=sendrecv");
            String videoChannelId = Arrays.stream(videoMediaLines)
                    .map(midAttributePattern::matcher)
                    .filter(Matcher::matches)
                    .map(x -> x.group("channelId"))
                    .findFirst()
                    .map(x -> x.replaceAll("^video-mixed-", ""))
                    .orElse(null);
            List<Long> videoSsrcList = Arrays.stream(videoMediaLines)
                    .map(ssrcPattern::matcher)
                    .filter(Matcher::matches)
                    .map(x -> x.group("ssrcId"))
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toList());
            List<Map.Entry<String, List<Long>>> videoSsrcGroupList = Arrays.stream(videoMediaLines)
                    .map(ssrcGroupPattern::matcher)
                    .filter(Matcher::matches)
                    .map(x-> Map.entry(x.group("semantic"), Arrays.stream(x.group("ssrcIdList").split("\\s+")).map(Long::parseLong).collect(Collectors.toList())))
                    .collect(Collectors.toList());

            String[] dataMediaLines = SdpUtils.getMediaDescriptionLines(sdpSections, "application", "");

            String[] confIdMediaLines = SdpUtils.getMediaDescriptionLines(sdpSections, "text", "\r\na=mid:confId-");

            String conferenceId;
            String sctpConnectionId;
            if (confIdMediaLines.length == 0) {
                Matcher midDataAttributeMatcher = Arrays.stream(dataMediaLines)
                        .map(midDataAttributeDeprecatedPattern::matcher)
                        .filter(Matcher::matches)
                        .findFirst().orElseThrow();

                conferenceId = midDataAttributeMatcher.group("conferenceId");
                sctpConnectionId = midDataAttributeMatcher.group("sctpConnectionId");
                sctpConnectionId = sctpConnectionId.replaceAll("$data-", "");
            } else {
                Matcher midConfIdAttributeMatcher = Arrays.stream(confIdMediaLines)
                        .map(midConfIdAttributePattern::matcher)
                        .filter(Matcher::matches)
                        .findFirst().orElseThrow();
                conferenceId = midConfIdAttributeMatcher.group("conferenceId");

                Matcher midDataAttributeMatcher = Arrays.stream(dataMediaLines)
                        .map(midDataAttributePattern::matcher)
                        .filter(Matcher::matches)
                        .findFirst().orElseThrow();

                sctpConnectionId = midDataAttributeMatcher.group("sctpConnectionId");
                sctpConnectionId = sctpConnectionId.replaceAll("$data-", "");
            }

            String iceUfrag = Arrays.stream(dataMediaLines)
                    .filter(x -> x.startsWith("a=ice-ufrag:"))
                    .map(x -> x.replace("a=ice-ufrag:", ""))
                    .findFirst().orElseThrow(() -> new ProgramLogicException("No 'ice-ufrag' attribute"));

            String icePwdAttribute = Arrays.stream(dataMediaLines)
                    .filter(x -> x.startsWith("a=ice-pwd:"))
                    .map(x -> x.replace("a=ice-pwd:", ""))
                    .findFirst().orElseThrow(() -> new ProgramLogicException("No 'ice-pwd' attribute"));
            String icePwd = icePwdAttribute.replace("a=ice-pwd:", "");

            String[] fingerprintAttribute = Stream.concat(Arrays.stream(sdpSections[0].split("\r\n")), Arrays.stream(dataMediaLines))
                    .filter(x -> x.startsWith("a=fingerprint:"))
                    .map(x -> x.replace("a=fingerprint:", ""))
                    .map(x -> x.split(" "))
                    .findFirst().orElse(null);
            if (fingerprintAttribute != null && fingerprintAttribute.length != 2) {
                throw new ProgramLogicException("Strange fingerprint attribute");
            }
            String fingerprintHash = fingerprintAttribute != null ? fingerprintAttribute[0] : null;

            String fingerprintValue = fingerprintAttribute != null ? fingerprintAttribute[1] : null;

            if (Stream.of(fingerprintHash, fingerprintValue).anyMatch(x -> x == null || x.length() == 0)) {
                throw new RuntimeException("fingerprint error");
            }

            String fingerprintSetup = Arrays.stream(dataMediaLines)
                    .filter(x -> x.startsWith("a=setup:"))
                    .map(x -> x.replace("a=setup:", ""))
                    .findFirst().orElse(null);

            List<Answer.Candidate> candidates = Arrays.stream(dataMediaLines)
                    .map(candidatePattern::matcher)
                    .filter(Matcher::matches)
                    .map(x -> Answer.Candidate.builder()
                            .foundation(x.group("foundation"))
                            .componentId(x.group("componentId"))
                            .transport(x.group("transport"))
                            .priority(x.group("priority"))
                            .connectionAddress(x.group("connectionAddress"))
                            .port(x.group("port"))
                            .candidateType(x.group("candidateType"))
                            .relAddress(x.group("relAddress"))
                            .relPort(x.group("relPort"))
                            .generation(x.group("generation"))
                            .build())
                    .collect(Collectors.toList());

            answers.add(Answer.builder()
                    .conferenceId(conferenceId)
                    .sctpConnectionId(sctpConnectionId)
                    .audioChannelId(audioChannelId)
                    .audioSsrc(audioSsrcList)
                    .audioSsrcGroups(audioSsrcGroupList)
                    .videoChannelId(videoChannelId)
                    .videoSsrc(videoSsrcList)
                    .videoSsrcGroups(videoSsrcGroupList)
                    .fingerprintValue(fingerprintValue)
                    .fingerprintHash(fingerprintHash)
                    .fingerprintSetup(fingerprintSetup)
                    .iceUfrag(iceUfrag)
                    .icePwd(icePwd)
                    .candidates(candidates)
                    .build());

            log.trace("SDP answer details:\n{}", sdpSections[0]);
            log.trace(
                    "SDP answer parsed parameters: conferenceId={}, sctpConnectionId={}, audioChannelId={}, audioSsrcList={}, videoChannelId={}, videoSsrcList={}",
                    conferenceId,
                    sctpConnectionId,
                    audioChannelId,
                    audioSsrcList,
                    videoChannelId,
                    videoSsrcList
            );
        }

        videobridgeService.processAnswers(authToken.getConferenceGid(), authToken.getEndpoint(), answers);
    }

    @Data
    public static class IceCandidate {
        private String candidate;
        private String sdpMid;
        private String sdpMLineIndex;
    }

    @PostMapping(value = "/icecandidate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    @MethodArgumentsLogger
    public void processIceCandidate(
            @RequestBody IceCandidate iceCandidate,
            VideobridgeAuthenticationToken authToken
    ) {
        Matcher matcher = midDataAttributePattern.matcher("a=mid:" + iceCandidate.getSdpMid());
        if (!matcher.matches()) {
            throw new RuntimeException("mid is not data");
        }
        String conferenceId = matcher.group("sctpConnectionId");

        matcher = candidatePattern.matcher("a=" + iceCandidate.getCandidate());
        if (!matcher.matches()) {
            throw new RuntimeException("candidate is not correct");
        }
        String foundation = matcher.group("foundation");
        String componentId = matcher.group("componentId");
        String transport = matcher.group("transport");
        String priority = matcher.group("priority");
        String connectionAddress = matcher.group("connectionAddress");
        String port = matcher.group("port");
        String candidateType = matcher.group("candidateType");
        String relAddress = matcher.group("relAddress");
        String relPort = matcher.group("relPort");
        String generation = matcher.group("generation");

        videobridgeService.processIceCandidate(
                authToken.getConferenceGid(),
                authToken.getEndpoint(),
                conferenceId,
                foundation,
                Integer.parseInt(componentId),
                gson.fromJson(transport, Candidate.Protocol.class),
                Integer.parseInt(priority),
                connectionAddress,
                Integer.parseInt(port),
                gson.fromJson(candidateType, Candidate.Type.class),
                relAddress,
                relPort != null ? Integer.parseInt(relPort) : null,
                generation
        );
    }

    @PatchMapping(value = "/offers", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @MethodArgumentsLogger
    public String getUpdatedOffers(
            @RequestBody String prevSdpOffers,
            @RequestParam(value = "videoBandwidth", required = false, defaultValue = "200") Long videoBandwidth,
            VideobridgeAuthenticationToken authToken
    ) {
        List<PrevOffer> prevOffers = new ArrayList<>();
        for (String prevSdpOffer : prevSdpOffers.split("\r\n(?=v=)")) {
            String[] sdpSections = SdpUtils.splitSdpIntoSections(prevSdpOffer);
            Origin originField = SdpUtils.getOrigin(sdpSections);

            String[] dataMediaLines = SdpUtils.getMediaDescriptionLines(sdpSections, "application", "data-");

            Matcher applicationMidAttributeMatcher = Arrays.stream(dataMediaLines)
                    .map(midDataAttributePattern::matcher)
                    .filter(Matcher::matches)
                    .findFirst().orElseThrow();

            String conferenceId = applicationMidAttributeMatcher.group("conferenceId");

            PrevOffer prevOffer = PrevOffer.builder()
                    .conferenceId(conferenceId)
                    .sessionId(originField.getSessId())
                    .sessionVersion(originField.getSessVersion())
                    .channels(SdpUtils.getOtherMediaIds(sdpSections))
                    .build();
            prevOffers.add(prevOffer);
            log.trace("SDP previous offer details:\n{}", sdpSections[0]);
            log.trace(
                    "SDP previous offer parsed parameters: conferenceId={}, sessionId={}, sessionVersion={}, channels={}",
                    conferenceId,
                    prevOffer.getSessionId(),
                    prevOffer.getSessionVersion(),
                    prevOffer.getChannels()
            );

        }

        return serializeOffers(videobridgeService.getOffers(authToken.getConferenceGid(), authToken.getEndpoint(), videoBandwidth, prevOffers));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    @MethodArgumentsLogger
    public void delete(VideobridgeAuthenticationToken authToken) {
        videobridgeService.delete(authToken.getConferenceGid(), authToken.getEndpoint(), false);
    }


    @ExceptionHandler(BadSdpException.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleException(BadSdpException ex) {
        return ex.toString();
    }

    @ExceptionHandler(EndpointNotFound.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public String handleException(EndpointNotFound ex) {
        return "Requested endpoint is not exist in conference";
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public String handleException(ConflictException ex) {
        return ex.toString();
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex) {
        log.error("SignalingController exception", ex);
        return ex.toString();
    }

}