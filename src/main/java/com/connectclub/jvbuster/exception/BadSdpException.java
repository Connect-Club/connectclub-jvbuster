package com.connectclub.jvbuster.exception;

public class BadSdpException extends RuntimeException {

    public BadSdpException() {
        super();
    }

    public BadSdpException(String s) {
        super(s);
    }

    public BadSdpException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadSdpException(Throwable cause) {
        super(cause);
    }

}
