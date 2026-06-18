package com.flashcard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed binding for the {@code app.*} configuration keys (see application.yml).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String adminEmail,
        Jwt jwt,
        Cookie cookie
) {
    public record Jwt(
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {}

    public record Cookie(
            boolean secure
    ) {}
}
