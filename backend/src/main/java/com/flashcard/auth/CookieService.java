package com.flashcard.auth;

import com.flashcard.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Builds the auth cookies. Both tokens are HttpOnly (never readable by JavaScript). The
 * access cookie is scoped to {@code /api}; the refresh cookie is scoped to {@code /api/auth}
 * so it is only sent to auth endpoints, not on every API call. SameSite=Lax plus the CSRF
 * double-submit token guards against cross-site request forgery.
 */
@Service
public class CookieService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/auth";

    private final AppProperties appProperties;

    public CookieService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public ResponseCookie accessCookie(String token) {
        return base(ACCESS_COOKIE, token, ACCESS_PATH)
                .maxAge(appProperties.jwt().accessTokenTtl())
                .build();
    }

    public ResponseCookie refreshCookie(String token) {
        return base(REFRESH_COOKIE, token, REFRESH_PATH)
                .maxAge(appProperties.jwt().refreshTokenTtl())
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return base(ACCESS_COOKIE, "", ACCESS_PATH).maxAge(0).build();
    }

    public ResponseCookie clearRefreshCookie() {
        return base(REFRESH_COOKIE, "", REFRESH_PATH).maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .sameSite("Lax")
                .path(path);
    }
}
