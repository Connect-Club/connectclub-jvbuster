package com.connectclub.jvbuster.exception;

public class EndpointNotFound extends RuntimeException {

    public EndpointNotFound() {
        super();
    }

    public EndpointNotFound(String message) {
        super(message);
    }

    public EndpointNotFound(Throwable cause) {
        super(cause);
    }

}
