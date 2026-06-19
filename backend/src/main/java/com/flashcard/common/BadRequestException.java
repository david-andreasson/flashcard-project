package com.flashcard.common;

/**
 * Thrown for semantically invalid requests that Bean Validation cannot express (e.g. a
 * cross-field rule). Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
