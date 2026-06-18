package com.flashcard.common;

/**
 * Thrown when a resource does not exist OR the caller is not allowed to see it. Mapped to
 * HTTP 404 — deliberately not 403, so the response does not reveal whether the id exists.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
