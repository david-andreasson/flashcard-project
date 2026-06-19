package com.flashcard.ai;

/**
 * A provider-agnostic AI request. {@code maxTokens} caps the output so a single call cannot
 * produce unbounded text; {@link AiService} fills in the configured default when it is null.
 */
public record AiRequest(String systemPrompt, String userMessage, Integer maxTokens) {
}
