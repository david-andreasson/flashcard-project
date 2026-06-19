package com.flashcard.common;

/** Thrown when a feature is temporarily turned off (e.g. the AI kill-switch). Mapped to HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
