package com.flashcard.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to generate flashcard drafts from a block of text. {@code count} is an optional upper
 * bound on how many cards to produce. The text size limit is enforced by the AI pipeline
 * (`ai.max-input-chars`), not here, so an oversized paste surfaces as the pipeline's 400.
 */
public record GenerateCardsRequest(@NotBlank String text, Integer count) {
}
