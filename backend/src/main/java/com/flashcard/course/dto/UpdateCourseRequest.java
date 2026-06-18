package com.flashcard.course.dto;

import com.flashcard.course.Visibility;
import jakarta.validation.constraints.Size;

/**
 * Partial update — null fields are left unchanged. {@code visibility = PUBLIC} requires ADMIN
 * (enforced in the service).
 */
public record UpdateCourseRequest(
        @Size(max = 200) String title,
        Visibility visibility
) {
}
