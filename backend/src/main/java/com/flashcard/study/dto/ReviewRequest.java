package com.flashcard.study.dto;

import com.flashcard.study.Grade;
import jakarta.validation.constraints.NotNull;

/** A review submission: how the user graded their recall of the card. */
public record ReviewRequest(@NotNull Grade grade) {
}
