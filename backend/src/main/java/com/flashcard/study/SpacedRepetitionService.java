package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.Card;
import com.flashcard.card.CardRepository;
import com.flashcard.card.dto.CardResponse;
import com.flashcard.common.NotFoundException;
import com.flashcard.course.CourseService;
import com.flashcard.deck.DeckRepository;
import com.flashcard.study.dto.ProgressResponse;
import com.flashcard.study.dto.ReviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Spaced-repetition operations: grade a card (SM-2), list a deck's due cards, and summarise
 * progress. Access mirrors study/cards — a deck must be readable (owned or PUBLIC), otherwise 404.
 */
@Service
public class SpacedRepetitionService {

    /** Upper bound on cards returned by one due-queue request, so a session stays bounded. */
    private static final int MAX_DUE = 200;

    private final UserCardStateRepository stateRepository;
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CourseService courseService;

    public SpacedRepetitionService(UserCardStateRepository stateRepository,
                                   CardRepository cardRepository,
                                   DeckRepository deckRepository,
                                   CourseService courseService) {
        this.stateRepository = stateRepository;
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.courseService = courseService;
    }

    @Transactional
    public ReviewResponse review(AuthPrincipal principal, Long courseId, Long deckId, Long cardId, Grade grade) {
        Card card = requireReadableCard(principal, courseId, deckId, cardId);
        UserCardState state = stateRepository.findByUserIdAndCardId(principal.id(), card.getId())
                .orElseGet(() -> new UserCardState(principal.id(), card.getId()));

        Instant now = Instant.now();
        Sm2Scheduler.Schedule next = Sm2Scheduler.next(
                state.getEaseFactor(), state.getIntervalDays(), state.getRepetitions(), grade, now);
        state.setEaseFactor(next.easeFactor());
        state.setIntervalDays(next.intervalDays());
        state.setRepetitions(next.repetitions());
        state.setDueAt(next.dueAt());
        state.setLastReviewedAt(now);

        UserCardState saved = stateRepository.save(state);
        return new ReviewResponse(saved.getIntervalDays(), saved.getDueAt(),
                saved.getRepetitions(), saved.getEaseFactor());
    }

    @Transactional(readOnly = true)
    public List<CardResponse> due(AuthPrincipal principal, Long courseId, Long deckId) {
        requireReadableDeck(principal, courseId, deckId);
        return stateRepository.findDueCards(principal.id(), deckId, Instant.now()).stream()
                .limit(MAX_DUE)
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressResponse progress(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long dueNow = stateRepository.countByUserIdAndDueAtLessThanEqual(principal.id(), now);
        long inReview = stateRepository.countByUserId(principal.id());
        long reviewedToday = stateRepository.countByUserIdAndLastReviewedAtGreaterThanEqual(principal.id(), startOfToday);
        return new ProgressResponse(dueNow, inReview, reviewedToday);
    }

    private Card requireReadableCard(AuthPrincipal principal, Long courseId, Long deckId, Long cardId) {
        requireReadableDeck(principal, courseId, deckId);
        return cardRepository.findByIdAndDeckId(cardId, deckId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private void requireReadableDeck(AuthPrincipal principal, Long courseId, Long deckId) {
        courseService.getReadable(courseId, principal.id()); // 404 if not readable
        deckRepository.findByIdAndCourseId(deckId, courseId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
    }
}
