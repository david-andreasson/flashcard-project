package com.flashcard.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A record of one completed study run: which deck, how many cards, how many correct. Holds no
 * per-card state — spaced-repetition scheduling is change 08.
 */
@Entity
@Table(name = "study_sessions")
@Getter
@Setter
@NoArgsConstructor
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "total_cards", nullable = false)
    private int totalCards;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StudySession(Long userId, Long deckId, int totalCards, int correctCount, Instant finishedAt) {
        this.userId = userId;
        this.deckId = deckId;
        this.totalCards = totalCards;
        this.correctCount = correctCount;
        this.finishedAt = finishedAt;
    }
}
