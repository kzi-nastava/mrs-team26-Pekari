package com.pekara.exception;

public class NoActiveDriversException extends RuntimeException {
    public NoActiveDriversException(String message) {
        super(message);
    }
}
