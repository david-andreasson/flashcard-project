package com.flashcard.deck;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.NotFoundException;
import com.flashcard.course.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deck operations resolve the parent course's access first (read or write), then act on the
 * deck. A deck has no independent visibility — it follows its course.
 */
@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final CourseService courseService;

    public DeckService(DeckRepository deckRepository, CourseService courseService) {
        this.deckRepository = deckRepository;
        this.courseService = courseService;
    }

    @Transactional
    public Deck create(Long courseId, AuthPrincipal principal, String title) {
        courseService.getWritable(courseId, principal); // 404 if not writable
        return deckRepository.save(new Deck(courseId, title));
    }

    @Transactional(readOnly = true)
    public Page<Deck> list(Long courseId, AuthPrincipal principal, Pageable pageable) {
        courseService.getReadable(courseId, principal.id()); // 404 if not readable
        return deckRepository.findByCourseId(courseId, pageable);
    }

    @Transactional(readOnly = true)
    public Deck get(Long courseId, Long deckId, AuthPrincipal principal) {
        courseService.getReadable(courseId, principal.id());
        return findInCourse(deckId, courseId);
    }

    @Transactional
    public Deck update(Long courseId, Long deckId, AuthPrincipal principal, String title) {
        courseService.getWritable(courseId, principal);
        Deck deck = findInCourse(deckId, courseId);
        deck.setTitle(title);
        return deckRepository.save(deck);
    }

    @Transactional
    public void delete(Long courseId, Long deckId, AuthPrincipal principal) {
        courseService.getWritable(courseId, principal);
        Deck deck = findInCourse(deckId, courseId);
        deckRepository.delete(deck);
    }

    private Deck findInCourse(Long deckId, Long courseId) {
        return deckRepository.findByIdAndCourseId(deckId, courseId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
    }
}
