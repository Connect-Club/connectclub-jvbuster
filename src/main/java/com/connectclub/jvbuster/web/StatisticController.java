package com.connectclub.jvbuster.web;

import com.connectclub.jvbuster.repository.data.StatsEndpointData;
import com.connectclub.jvbuster.repository.data.StatsSubscribedEndpointData;
import com.connectclub.jvbuster.repository.i.StatsEndpointDataRepository;
import com.connectclub.jvbuster.repository.i.StatsSubscribedEndpointDataRepository;
import com.connectclub.jvbuster.web.data.EndpointStats;
import com.connectclub.jvbuster.web.data.WebEndpointStats;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/statistic")
public class StatisticController {

    private final StatsEndpointDataRepository statsEndpointDataRepository;
    private final StatsSubscribedEndpointDataRepository statsSubscribedEndpointDataRepository;

    public StatisticController(
            StatsEndpointDataRepository statsEndpointDataRepository,
            StatsSubscribedEndpointDataRepository statsSubscribedEndpointDataRepository) {
        this.statsEndpointDataRepository = statsEndpointDataRepository;
        this.statsSubscribedEndpointDataRepository = statsSubscribedEndpointDataRepository;
    }

    private boolean logIfNeed(EndpointStats endpointStats, boolean alreadyLogged) {
        if (!alreadyLogged) {
            log.warn("Fraction lost fixed:\n{}", endpointStats);
        }
        return true;
    }

    @PostMapping(value = "/endpoint", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void postEndpointStats(@RequestBody EndpointStats endpointStats) {
        boolean alreadyLogged = false;

        if (endpointStats.getFractionLost() > Byte.MAX_VALUE) {
            alreadyLogged = logIfNeed(endpointStats, false);
            endpointStats.setFractionLost(Byte.MAX_VALUE);
        } else if (endpointStats.getFractionLost() < 0) {
            alreadyLogged = logIfNeed(endpointStats, false);
            endpointStats.setFractionLost(0);
        }

        for (EndpointStats.SubscribedEndpoint subscribedEndpoint : endpointStats.getSubscribedEndpoints()) {
            if (subscribedEndpoint.getFractionLost() > Byte.MAX_VALUE) {
                alreadyLogged = logIfNeed(endpointStats, alreadyLogged);
                subscribedEndpoint.setFractionLost(Byte.MAX_VALUE);
            } else if (subscribedEndpoint.getFractionLost() < 0) {
                alreadyLogged = logIfNeed(endpointStats, alreadyLogged);
                subscribedEndpoint.setFractionLost(0);
            }
        }

        StatsEndpointData statsEndpointData = statsEndpointDataRepository.save(
                StatsEndpointData.builder()
                        .createdAt(Instant.ofEpochSecond(endpointStats.getCreatedAt()))
                        .conferenceId(endpointStats.getConferenceId())
                        .conferenceGid(endpointStats.getConferenceGid())
                        .endpointId(endpointStats.getEndpointId())
                        .endpointUuid(endpointStats.getEndpointUuid())
                        .rtt(endpointStats.getRtt())
                        .jitter(endpointStats.getJitter())
                        .expectedPackets(endpointStats.getExpectedPackets())
                        .fractionLost((byte) endpointStats.getFractionLost())
                        .build()
        );
        statsSubscribedEndpointDataRepository.saveAll(
                endpointStats.getSubscribedEndpoints().stream()
                        .map(x -> StatsSubscribedEndpointData.builder()
                                .statsEndpoint(statsEndpointData)
                                .endpointId(x.getEndpointId())
                                .expectedPackets(x.getExpectedPackets())
                                .fractionLost((byte) x.getFractionLost())
                                .build())
                        .collect(Collectors.toList())
        );
    }

    @GetMapping(value = "/endpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional(readOnly = true)
    public @ResponseBody
    List<EndpointStats> getEndpointStats(
            @RequestParam(value = "conferenceGid") String conferenceGid,
            @RequestParam(value = "endpointId") String endpointId
    ) {
        return statsEndpointDataRepository.findAllByConferenceGidAndEndpointIdOrderByCreatedAt(conferenceGid, endpointId).stream()
                .map(x -> EndpointStats.builder()
                        .createdAt(x.getCreatedAt().getEpochSecond())
                        .conferenceId(x.getConferenceId())
                        .conferenceGid(x.getConferenceGid())
                        .endpointId(x.getEndpointId())
                        .endpointUuid(x.getEndpointUuid())
                        .rtt(x.getRtt())
                        .jitter(x.getJitter())
                        .expectedPackets(x.getExpectedPackets())
                        .fractionLost(x.getFractionLost())
                        .subscribedEndpoints(
                                x.getSubscribedEndpoints().stream()
                                        .map(s -> EndpointStats.SubscribedEndpoint.builder()
                                                .endpointId(s.getEndpointId())
                                                .expectedPackets(s.getExpectedPackets())
                                                .fractionLost(s.getFractionLost())
                                                .build())
                                        .collect(Collectors.toList())
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/web", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional(readOnly = true)
    public @ResponseBody
    Map<String, List<WebEndpointStats>> getWebEndpointStats(
            @RequestParam(value = "conferenceGid") String conferenceGid,
            @RequestParam(value = "endpointId", required = false) String endpointId
    ) {
        List<StatsEndpointData> stats;
        if (Strings.isBlank(endpointId)) {
            stats = statsEndpointDataRepository.findAllByConferenceGidOrderByCreatedAt(conferenceGid);
        } else {
            stats = statsEndpointDataRepository.findAllByConferenceGidAndEndpointIdOrderByCreatedAt(conferenceGid, endpointId);
        }
        return stats.stream()
                .collect(Collectors.groupingBy(
                        StatsEndpointData::getEndpointId,
                        Collectors.mapping(StatisticController::toWebEndpointStats, Collectors.toList())
                ));
    }

    private static WebEndpointStats toWebEndpointStats(StatsEndpointData data) {
        return WebEndpointStats.builder()
                .id(data.getId())
                .createdAt(data.getCreatedAt().getEpochSecond())
                .rtt(data.getRtt()).fractionLost(data.getFractionLost())
                .subscribedEndpoints(
                        data.getSubscribedEndpoints().stream()
                                .map(x -> WebEndpointStats.SubscribedEndpoint.builder()
                                        .endpointId(x.getEndpointId())
                                        .fractionLost(x.getFractionLost())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex) {
        log.error("StatisticController exception", ex);
        return ex.toString();
    }
}
