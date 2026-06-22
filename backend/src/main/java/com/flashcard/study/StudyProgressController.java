package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.study.dto.ProgressResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The current user's spaced-repetition progress summary. */
@RestController
@RequestMapping("/study")
public class StudyProgressController {

    private final SpacedRepetitionService service;

    public StudyProgressController(SpacedRepetitionService service) {
        this.service = service;
    }

    @GetMapping("/progress")
    public ProgressResponse progress(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.progress(principal);
    }
}
