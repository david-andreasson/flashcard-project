package com.flashcard.ai;

/**
 * A provider-agnostic AI response. The token counts drive usage logging and quota enforcement,
 * so every provider must report them.
 */
public record AiResponse(String content, int inputTokens, int outputTokens, String modelId) {

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
