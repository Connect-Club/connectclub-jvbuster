package com.connectclub.jvbuster.exception;

public class NoJVBAvailableException extends RuntimeException {

    public NoJVBAvailableException() {
        super();
    }

    public NoJVBAvailableException(String s) {
        super(s);
    }

    public NoJVBAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoJVBAvailableException(Throwable cause) {
        super(cause);
    }

}
