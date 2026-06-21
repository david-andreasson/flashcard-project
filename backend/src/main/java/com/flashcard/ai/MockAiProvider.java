package com.flashcard.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, free provider used for development and tests. It echoes a transformed version
 * of the input and reports plausible token counts, so the whole AI pipeline — gating, quotas,
 * logging — is exercisable without any external API or key. For the {@code card-generation}
 * feature it returns a small JSON array of drafts, so that feature is exercisable end to end with
 * the mock too. The real providers map a live response into the same {@link AiResponse} shape.
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {

    static final String MODEL_ID = "mock-1";

    /** Two deterministic drafts — valid JSON for the card-generation parser. */
    private static final String MOCK_DRAFTS_JSON =
            "[{\"front\":\"[mock] Q1\",\"back\":\"[mock] A1\"},"
            + "{\"front\":\"[mock] Q2\",\"back\":\"[mock] A2\"}]";

    @Override
    public AiResponse complete(AiRequest request) {
        String system = request.systemPrompt() == null ? "" : request.systemPrompt();
        String user = request.userMessage() == null ? "" : request.userMessage();

        String content = "card-generation".equals(request.featureKey())
                ? MOCK_DRAFTS_JSON
                : "[mock] " + user;

        int inputTokens = TokenEstimator.estimate(system) + TokenEstimator.estimate(user);
        int outputTokens = TokenEstimator.estimate(content);
        return new AiResponse(content, inputTokens, outputTokens, MODEL_ID);
    }
}
