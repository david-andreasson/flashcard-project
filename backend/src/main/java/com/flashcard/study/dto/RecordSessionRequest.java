package com.flashcard.study.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordSessionRequest(
        @NotNull Long deckId,
        @NotNull @Min(0) Integer totalCards,
        @NotNull @Min(0) Integer correctCount
) {
}
