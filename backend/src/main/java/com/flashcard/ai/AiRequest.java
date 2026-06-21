package com.flashcard.ai;

/**
 * A provider-agnostic AI request. {@code maxTokens} caps the output so a single call cannot
 * produce unbounded text; {@link AiService} fills in the configured default when it is null and
 * stamps the {@code featureKey} so a provider (e.g. the mock) can vary its output by feature.
 * Features use the three-argument constructor and never set the feature key themselves.
 */
public record AiRequest(String systemPrompt, String userMessage, Integer maxTokens, String featureKey) {

    public AiRequest(String systemPrompt, String userMessage, Integer maxTokens) {
        this(systemPrompt, userMessage, maxTokens, null);
    }
}
