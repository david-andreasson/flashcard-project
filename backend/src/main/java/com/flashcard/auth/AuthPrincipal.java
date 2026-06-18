package com.flashcard.auth;

import com.flashcard.user.Plan;
import com.flashcard.user.Role;

/**
 * The authenticated user, derived from the access-token claims. Set as the Spring Security
 * principal so controllers can read the current user's id/role/plan with
 * {@code @AuthenticationPrincipal} — no database lookup required.
 */
public record AuthPrincipal(Long id, String email, Role role, Plan plan) {
}
