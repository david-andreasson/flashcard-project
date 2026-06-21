package com.flashcard.ai.dto;

import java.util.List;

/**
 * The generated drafts plus the call's token usage. Usage is estimated when the provider does not
 * report it (e.g. 1min.ai), and is surfaced so the UI can show roughly what the call cost.
 */
public record GenerateCardsResponse(List<CardDraft> drafts, int inputTokens, int outputTokens, String model) {
}
