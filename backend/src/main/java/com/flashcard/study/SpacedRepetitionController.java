package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.dto.CardResponse;
import com.flashcard.study.dto.ReviewRequest;
import com.flashcard.study.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Spaced-repetition endpoints scoped to a deck: review a card (grade → SM-2 schedule) and fetch the
 * deck's due-and-new cards for the current user.
 */
@RestController
@RequestMapping("/courses/{courseId}/decks/{deckId}")
public class SpacedRepetitionController {

    private final SpacedRepetitionService service;

    public SpacedRepetitionController(SpacedRepetitionService service) {
        this.service = service;
    }

    @PostMapping("/cards/{cardId}/review")
    public ReviewResponse review(@AuthenticationPrincipal AuthPrincipal principal,
                                 @PathVariable Long courseId,
                                 @PathVariable Long deckId,
                                 @PathVariable Long cardId,
                                 @Valid @RequestBody ReviewRequest request) {
        return service.review(principal, courseId, deckId, cardId, request.grade());
    }

    @GetMapping("/due")
    public List<CardResponse> due(@AuthenticationPrincipal AuthPrincipal principal,
                                  @PathVariable Long courseId,
                                  @PathVariable Long deckId) {
        return service.due(principal, courseId, deckId);
    }
}
