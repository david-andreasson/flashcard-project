package com.flashcard.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record EchoRequest(
        @NotBlank String message
) {
}
