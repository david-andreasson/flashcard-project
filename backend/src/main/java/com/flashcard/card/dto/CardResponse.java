package com.flashcard.card.dto;

import com.flashcard.card.Card;

import java.time.Instant;

public record CardResponse(
        Long id,
        Long deckId,
        String front,
        String back,
        String notes,
        Instant createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(), card.getDeckId(), card.getFront(), card.getBack(),
                card.getNotes(), card.getCreatedAt());
    }
}
