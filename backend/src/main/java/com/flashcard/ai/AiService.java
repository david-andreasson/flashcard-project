package com.flashcard.ai;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.BadRequestException;
import com.flashcard.common.ForbiddenException;
import com.flashcard.common.QuotaExceededException;
import com.flashcard.common.ServiceUnavailableException;
import com.flashcard.user.Plan;
import com.flashcard.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The single guarded pipeline every AI feature must call. In order: kill-switch (503) → plan
 * gate (403) → input-size limit (400) → monthly token quota (429) → provider call → usage log.
 * Features pass their own {@code featureKey}; they never touch the provider, quota, or log.
 */
@Service
public class AiService {

    private final AiProvider provider;
    private final AiProperties aiProperties;
    private final AiQuotaService quotaService;
    private final AiUsageLogRepository usageLogRepository;

    public AiService(AiProvider provider,
                     AiProperties aiProperties,
                     AiQuotaService quotaService,
                     AiUsageLogRepository usageLogRepository) {
        this.provider = provider;
        this.aiProperties = aiProperties;
        this.quotaService = quotaService;
        this.usageLogRepository = usageLogRepository;
    }

    @Transactional
    public AiResponse complete(AuthPrincipal principal, String featureKey, AiRequest request) {
        // 1. Kill-switch
        if (!aiProperties.enabled()) {
            throw new ServiceUnavailableException("AI features are currently disabled");
        }
        // 2. Plan gate — PREMIUM or ADMIN only
        if (principal.plan() != Plan.PREMIUM && principal.role() != Role.ADMIN) {
            throw new ForbiddenException("AI features require a PREMIUM plan");
        }
        // 3. Input-size limit
        String message = request.userMessage() == null ? "" : request.userMessage();
        if (message.length() > aiProperties.maxInputChars()) {
            throw new BadRequestException(
                    "Input exceeds the maximum of " + aiProperties.maxInputChars() + " characters");
        }
        // 4. Monthly token quota
        if (quotaService.isOverQuota(principal)) {
            throw new QuotaExceededException("Monthly AI token quota reached");
        }
        // 5. Provider call — stamp the feature key and apply the default output cap when none is given
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : aiProperties.defaultMaxTokens();
        AiRequest effective = new AiRequest(
                request.systemPrompt(), request.userMessage(), maxTokens, featureKey);
        AiResponse response = provider.complete(effective);

        // 6. Log usage with estimated cost
        usageLogRepository.save(new AiUsageLog(
                principal.id(), featureKey, response.inputTokens(), response.outputTokens(),
                estimateCost(response.inputTokens(), response.outputTokens())));
        return response;
    }

    private BigDecimal estimateCost(int inputTokens, int outputTokens) {
        AiProperties.Pricing p = aiProperties.pricing();
        return p.inputRate().multiply(BigDecimal.valueOf(inputTokens))
                .add(p.outputRate().multiply(BigDecimal.valueOf(outputTokens)));
    }
}
