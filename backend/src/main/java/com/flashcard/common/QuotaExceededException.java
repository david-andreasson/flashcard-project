package com.flashcard.common;

/** Thrown when a user has reached a usage quota. Mapped to HTTP 429. */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
