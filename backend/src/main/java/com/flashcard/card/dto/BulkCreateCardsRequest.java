package com.flashcard.card.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A batch of cards to create in one deck. {@code @Valid} cascades into each item, so a blank front
 * or back in any item rejects the whole batch with HTTP 400 before anything is saved.
 */
public record BulkCreateCardsRequest(
        @NotEmpty @Size(max = 200) @Valid List<CreateCardRequest> cards
) {
}
