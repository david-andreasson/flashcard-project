package com.flashcard.common;

/**
 * Thrown when the caller is authenticated and the resource is visible to them, but the
 * specific action is not allowed (e.g. a non-admin trying to make a course PUBLIC). Mapped to
 * HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
