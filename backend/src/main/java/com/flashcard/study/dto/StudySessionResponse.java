package com.flashcard.study.dto;

import com.flashcard.study.StudySession;

import java.time.Instant;

public record StudySessionResponse(
        Long id,
        Long deckId,
        String deckTitle,
        int totalCards,
        int correctCount,
        Instant finishedAt
) {
    public static StudySessionResponse from(StudySession s, String deckTitle) {
        return new StudySessionResponse(
                s.getId(), s.getDeckId(), deckTitle, s.getTotalCards(), s.getCorrectCount(), s.getFinishedAt());
    }
}
