package com.flashcard.ai;

/**
 * Abstraction over an AI text-completion backend. Features depend only on this interface, never
 * on a specific SDK. The active implementation is chosen by {@code ai.provider}.
 */
public interface AiProvider {

    AiResponse complete(AiRequest request);
}
