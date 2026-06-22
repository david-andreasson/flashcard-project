package com.flashcard.study.dto;

/** A user's spaced-repetition progress: cards due now, total in review, and reviewed today. */
public record ProgressResponse(long dueNow, long inReview, long reviewedToday) {
}
