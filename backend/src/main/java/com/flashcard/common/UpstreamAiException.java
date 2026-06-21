package com.flashcard.common;

/**
 * Thrown when an upstream AI provider call fails or returns a response that cannot be used (a
 * transport error, a non-success status, or output that cannot be parsed). Mapped to HTTP 502.
 */
public class UpstreamAiException extends RuntimeException {
    public UpstreamAiException(String message) {
        super(message);
    }

    public UpstreamAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
