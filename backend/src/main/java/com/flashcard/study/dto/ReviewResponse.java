package com.flashcard.study.dto;

import java.time.Instant;

/** The schedule produced by a review: the new interval, next-due date, and SM-2 bookkeeping. */
public record ReviewResponse(int intervalDays, Instant dueAt, int repetitions, double easeFactor) {
}
