package com.flashcard.auth.dto;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.user.Plan;
import com.flashcard.user.Role;
import com.flashcard.user.User;

/** Public view of a user. Never includes the password hash. */
public record UserResponse(Long id, String email, Role role, Plan plan) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getPlan());
    }

    public static UserResponse from(AuthPrincipal principal) {
        return new UserResponse(principal.id(), principal.email(), principal.role(), principal.plan());
    }
}
