package com.flashcard.ai;

import com.flashcard.user.Plan;
import com.flashcard.user.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Typed binding for the {@code ai.*} configuration (see application.yml): the kill-switch,
 * provider selection, input/output caps, price rates, and per-plan monthly token quotas.
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        boolean enabled,
        String provider,
        int maxInputChars,
        int defaultMaxTokens,
        Pricing pricing,
        Quota quota
) {
    public record Pricing(BigDecimal inputRate, BigDecimal outputRate) {}

    public record Quota(long free, long premium, long admin) {}

    /** Monthly token limit for a user, by plan/role (ADMIN gets the admin limit regardless of plan). */
    public long monthlyTokenLimit(Role role, Plan plan) {
        if (role == Role.ADMIN) {
            return quota.admin();
        }
        return plan == Plan.PREMIUM ? quota.premium() : quota.free();
    }
}
