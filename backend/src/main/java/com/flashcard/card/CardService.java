package com.flashcard.card;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.dto.CreateCardRequest;
import com.flashcard.common.NotFoundException;
import com.flashcard.course.CourseService;
import com.flashcard.deck.DeckRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Card operations resolve access through the hierarchy: the course (read = owned-or-public,
 * write = owned/ADMIN) → the deck within that course → the card within that deck. Anything not
 * accessible surfaces as 404. Cards carry no visibility of their own.
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CourseService courseService;

    public CardService(CardRepository cardRepository,
                       DeckRepository deckRepository,
                       CourseService courseService) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.courseService = courseService;
    }

    @Transactional
    public Card create(Long courseId, Long deckId, AuthPrincipal principal,
                       String front, String back, String notes) {
        courseService.getWritable(courseId, principal);
        requireDeckInCourse(deckId, courseId);
        return cardRepository.save(new Card(deckId, front, back, notes));
    }

    /**
     * Create many cards in one deck atomically. Access is checked once for the deck; bean
     * validation has already rejected any batch with a blank front or back before this runs.
     */
    @Transactional
    public List<Card> createBulk(Long courseId, Long deckId, AuthPrincipal principal,
                                 List<CreateCardRequest> items) {
        courseService.getWritable(courseId, principal);
        requireDeckInCourse(deckId, courseId);
        List<Card> cards = items.stream()
                .map(i -> new Card(deckId, i.front(), i.back(), i.notes()))
                .toList();
        return cardRepository.saveAll(cards);
    }

    @Transactional(readOnly = true)
    public Page<Card> list(Long courseId, Long deckId, AuthPrincipal principal, Pageable pageable) {
        courseService.getReadable(courseId, principal.id());
        requireDeckInCourse(deckId, courseId);
        return cardRepository.findByDeckId(deckId, pageable);
    }

    @Transactional(readOnly = true)
    public Card get(Long courseId, Long deckId, Long cardId, AuthPrincipal principal) {
        courseService.getReadable(courseId, principal.id());
        requireDeckInCourse(deckId, courseId);
        return requireCardInDeck(cardId, deckId);
    }

    @Transactional
    public Card update(Long courseId, Long deckId, Long cardId, AuthPrincipal principal,
                       String front, String back, String notes) {
        courseService.getWritable(courseId, principal);
        requireDeckInCourse(deckId, courseId);
        Card card = requireCardInDeck(cardId, deckId);
        card.setFront(front);
        card.setBack(back);
        card.setNotes(notes);
        return cardRepository.save(card);
    }

    @Transactional
    public void delete(Long courseId, Long deckId, Long cardId, AuthPrincipal principal) {
        courseService.getWritable(courseId, principal);
        requireDeckInCourse(deckId, courseId);
        Card card = requireCardInDeck(cardId, deckId);
        cardRepository.delete(card);
    }

    private void requireDeckInCourse(Long deckId, Long courseId) {
        deckRepository.findByIdAndCourseId(deckId, courseId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
    }

    private Card requireCardInDeck(Long cardId, Long deckId) {
        return cardRepository.findByIdAndDeckId(cardId, deckId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }
}
