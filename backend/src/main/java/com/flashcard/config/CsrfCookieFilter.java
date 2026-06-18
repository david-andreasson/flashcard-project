package com.flashcard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Materializes the CSRF token on each request so the {@code XSRF-TOKEN} cookie is written to
 * the response. Without this, {@code CookieCsrfTokenRepository} defers token creation and the
 * SPA never receives a cookie to echo back. This is the documented Spring Security 6 pattern
 * for cookie-based CSRF with a single-page app.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Accessing the token value triggers the repository to write the cookie.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
