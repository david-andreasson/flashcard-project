package com.flashcard.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, free provider used for development and tests. It echoes a transformed version
 * of the input and reports plausible token counts (~4 chars/token), so the whole AI pipeline —
 * gating, quotas, logging — is exercisable without any external API or key. The real Anthropic
 * provider (change 07) maps a live response into the same {@link AiResponse} shape.
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {

    static final String MODEL_ID = "mock-1";

    @Override
    public AiResponse complete(AiRequest request) {
        String system = request.systemPrompt() == null ? "" : request.systemPrompt();
        String user = request.userMessage() == null ? "" : request.userMessage();
        String content = "[mock] " + user;

        int inputTokens = estimateTokens(system) + estimateTokens(user);
        int outputTokens = estimateTokens(content);
        return new AiResponse(content, inputTokens, outputTokens, MODEL_ID);
    }

    /** Rough token estimate: ~4 characters per token, at least 1 for non-empty text. */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
