package com.connectclub.jvbuster.web;

import com.connectclub.jvbuster.exception.BadSdpException;
import com.connectclub.jvbuster.exception.ConflictException;
import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.security.VideobridgeAuthenticationToken;
import com.connectclub.jvbuster.videobridge.VideobridgeConferenceAnswer;
import com.connectclub.jvbuster.videobridge.data.VideobridgeConferenceOffer;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.connectclub.jvbuster.videobridge.i.NewVideobridgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/signaling-new")
public class SignalingJsonController {

    private final NewVideobridgeService newVideobridgeService;

    public SignalingJsonController(
            NewVideobridgeService newVideobridgeService
    ) {
        this.newVideobridgeService = newVideobridgeService;
    }

    @GetMapping(value = "/new-offers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<VideobridgeConferenceOffer> getNewOffers(
            VideobridgeAuthenticationToken authToken,
            @RequestParam(value = "speaker", defaultValue = "true") boolean speaker,
            HttpServletResponse response
    ) throws IOException, JvbInstanceRestException {
        if (authToken.isGuest() && speaker) {
            throw new ConflictException("a guest can not speak");
        }
        response.setHeader("Webrtc-Simulcast", "true");
        return newVideobridgeService.getNewOffers(authToken.getConferenceGid(), authToken.getEndpoint(), speaker);
    }

    @GetMapping(value = "/current-offers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<VideobridgeConferenceOffer> getCurrentOffers(
            VideobridgeAuthenticationToken authToken
    ) throws JvbInstanceRestException, IOException {
        return newVideobridgeService.getCurrentOffers(authToken.getConferenceGid(), authToken.getEndpoint());
    }

    @PostMapping(value = "/answers", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void processAnswers(
            VideobridgeAuthenticationToken authToken,
            @RequestBody List<VideobridgeConferenceAnswer> conferences
    ) throws IOException, JvbInstanceRestException {
        newVideobridgeService.processAnswers(authToken.getConferenceGid(), authToken.getEndpoint(), conferences);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void delete(VideobridgeAuthenticationToken authToken) {
        newVideobridgeService.delete(authToken.getConferenceGid(), authToken.getEndpoint(), false);
    }

    @PostMapping(value = "/log/error", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void log(@RequestBody String msg) {
        log.info("Client error: {}", msg);
    }

    @ExceptionHandler(EndpointNotFound.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public String handleException(EndpointNotFound ex) {
        log.info("SignalingJsonController exception", ex);
        if(ex.getMessage()==null) {
            return "The requested endpoint does not exist in the conference.";
        }
        return ex.getMessage();
    }

    @ExceptionHandler(BadSdpException.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleException(BadSdpException ex) {
        log.info("SignalingJsonController exception", ex);
        return ex.toString();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public String handleException(ConflictException ex) {
        log.info("SignalingJsonController exception", ex);
        return ex.toString();
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex) {
        log.error("SignalingJsonController exception", ex);
        return ex.toString();
    }
}
