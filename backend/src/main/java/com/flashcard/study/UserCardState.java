package com.flashcard.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Per-user spaced-repetition state for one card (SM-2). One row per (user, card), created lazily on
 * the user's first review, so a shared card schedules independently for each user. A card with no
 * row is "new" for that user.
 */
@Entity
@Table(name = "user_card_state",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_card", columnNames = {"user_id", "card_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserCardState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "last_reviewed_at", nullable = false)
    private Instant lastReviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** A fresh state for a card the user has not reviewed yet (SM-2 defaults). */
    public UserCardState(Long userId, Long cardId) {
        this.userId = userId;
        this.cardId = cardId;
        this.easeFactor = 2.5;
        this.intervalDays = 0;
        this.repetitions = 0;
    }
}
