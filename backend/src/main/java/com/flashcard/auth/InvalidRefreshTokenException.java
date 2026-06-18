package com.flashcard.auth;

/**
 * Thrown when a refresh token is missing, unknown, expired, or already rotated.
 * Mapped to HTTP 401 by the global exception handler.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
