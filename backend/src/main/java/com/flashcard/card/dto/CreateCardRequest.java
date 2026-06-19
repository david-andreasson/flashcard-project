package com.flashcard.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank @Size(max = 2000) String front,
        @NotBlank @Size(max = 2000) String back,
        @Size(max = 4000) String notes
) {
}
