package com.flashcard.ai;

import com.flashcard.auth.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Computes a user's current calendar-month token usage and compares it to their plan limit. */
@Service
public class AiQuotaService {

    private final AiUsageLogRepository usageLogRepository;
    private final AiProperties aiProperties;

    public AiQuotaService(AiUsageLogRepository usageLogRepository, AiProperties aiProperties) {
        this.usageLogRepository = usageLogRepository;
        this.aiProperties = aiProperties;
    }

    /** Tokens consumed by the user since the start of the current calendar month (UTC). */
    @Transactional(readOnly = true)
    public long monthToDateTokens(Long userId) {
        Instant monthStart = LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        return usageLogRepository.sumTokensSince(userId, monthStart);
    }

    /** True if the user has reached their plan's monthly token limit. */
    @Transactional(readOnly = true)
    public boolean isOverQuota(AuthPrincipal principal) {
        long used = monthToDateTokens(principal.id());
        long limit = aiProperties.monthlyTokenLimit(principal.role(), principal.plan());
        return used >= limit;
    }
}
