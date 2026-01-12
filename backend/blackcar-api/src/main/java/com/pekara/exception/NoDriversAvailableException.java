package com.pekara.exception;

public class NoDriversAvailableException extends RuntimeException {
    public NoDriversAvailableException(String message) {
        super(message);
    }
}
