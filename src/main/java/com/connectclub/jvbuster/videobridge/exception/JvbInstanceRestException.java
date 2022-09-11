package com.connectclub.jvbuster.videobridge.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
public class JvbInstanceRestException extends Exception {
    private final String id;

    private final String requestPath;
    private final String requestMethod;
    private final String requestBody;

    private final int responseCode;
    private final String responseMessage;
    private final String responseBody;

    public JvbInstanceRestException(String id, String requestPath, String requestMethod, String requestBody, int responseCode, String responseMessage, String responseBody) {
        super();

        this.id = id;

        this.requestPath = requestPath;
        this.requestMethod = requestMethod;
        this.requestBody = requestBody;

        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.responseBody = responseBody;
    }

    @Override
    public String getMessage() {
        return String.format(
                "JvbInstance(id=%s)\n" +
                        "Request(path=%s, method=%s, body=%s)\n" +
                        "Response(code=%s, message=%s, body=%s)",
                id, requestPath, requestMethod, requestBody, responseCode, responseMessage, responseBody
        );
    }
}
