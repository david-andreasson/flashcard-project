package com.flashcard.deck.dto;

import com.flashcard.deck.Deck;

import java.time.Instant;

public record DeckResponse(
        Long id,
        Long courseId,
        String title,
        Instant createdAt
) {
    public static DeckResponse from(Deck deck) {
        return new DeckResponse(deck.getId(), deck.getCourseId(), deck.getTitle(), deck.getCreatedAt());
    }
}
