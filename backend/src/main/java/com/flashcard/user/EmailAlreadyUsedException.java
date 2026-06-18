package com.flashcard.user;

/**
 * Thrown when registration is attempted with an email that already exists.
 * Mapped to HTTP 409 by the global exception handler.
 */
public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Email already in use: " + email);
    }
}
