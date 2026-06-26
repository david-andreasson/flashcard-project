package com.flashcard.course.dto;

import com.flashcard.course.Course;
import com.flashcard.course.Visibility;

import java.time.Instant;

public record CourseResponse(
        Long id,
        Long ownerId,
        String title,
        Visibility visibility,
        Instant createdAt,
        long deckCount,
        long cardCount
) {
    public static CourseResponse from(Course course) {
        return from(course, 0, 0);
    }

    public static CourseResponse from(Course course, long deckCount, long cardCount) {
        return new CourseResponse(
                course.getId(),
                course.getOwnerId(),
                course.getTitle(),
                course.getVisibility(),
                course.getCreatedAt(),
                deckCount,
                cardCount
        );
    }
}
