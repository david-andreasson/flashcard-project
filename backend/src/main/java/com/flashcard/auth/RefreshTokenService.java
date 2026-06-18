package com.flashcard.auth;

import com.flashcard.config.AppProperties;
import com.flashcard.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Manages long-lived refresh tokens. The raw token is returned to the caller (for the
 * cookie) but only its SHA-256 hash is persisted, so a database leak does not expose live
 * sessions. Tokens are rotated on each refresh: the used token is deleted and a new one
 * issued.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Url = Base64.getUrlEncoder().withoutPadding();

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    /** The new raw refresh token plus the user it belongs to. */
    public record Issued(User user, String rawToken) {}

    /** Issues a new refresh token for the user and stores its hash. Returns the raw token. */
    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        Instant expiresAt = Instant.now().plus(appProperties.jwt().refreshTokenTtl());
        repository.save(new RefreshToken(user, hash(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Validates a refresh token and rotates it: the presented token is deleted and a fresh
     * one is issued for the same user.
     *
     * @throws InvalidRefreshTokenException if the token is unknown or expired
     */
    @Transactional
    public Issued rotate(String rawToken) {
        RefreshToken existing = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            repository.delete(existing);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }
        User user = existing.getUser();
        repository.delete(existing);
        String newRaw = issue(user);
        return new Issued(user, newRaw);
    }

    /** Revokes a single refresh token (logout). No-op if the token is unknown. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null) {
            repository.deleteByTokenHash(hash(rawToken));
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return base64Url.encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
