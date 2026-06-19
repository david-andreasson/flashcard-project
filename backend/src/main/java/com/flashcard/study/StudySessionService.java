package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.BadRequestException;
import com.flashcard.common.NotFoundException;
import com.flashcard.course.CourseService;
import com.flashcard.deck.Deck;
import com.flashcard.deck.DeckRepository;
import com.flashcard.study.dto.StudySessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudySessionService {

    private final StudySessionRepository repository;
    private final DeckRepository deckRepository;
    private final CourseService courseService;

    public StudySessionService(StudySessionRepository repository,
                               DeckRepository deckRepository,
                               CourseService courseService) {
        this.repository = repository;
        this.deckRepository = deckRepository;
        this.courseService = courseService;
    }

    /**
     * Records a finished study session. The deck must be readable by the caller (own or public);
     * an unknown or unreadable deck both surface as 404. Counts must satisfy
     * {@code 0 <= correctCount <= totalCards}.
     */
    @Transactional
    public StudySessionResponse record(AuthPrincipal principal, Long deckId, int totalCards, int correctCount) {
        if (totalCards < 0 || correctCount < 0 || correctCount > totalCards) {
            throw new BadRequestException("correctCount must be between 0 and totalCards");
        }
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found"));
        courseService.getReadable(deck.getCourseId(), principal.id()); // 404 if not readable
        StudySession saved = repository.save(new StudySession(
                principal.id(), deckId, totalCards, correctCount, Instant.now()));
        return StudySessionResponse.from(saved, deck.getTitle());
    }

    @Transactional(readOnly = true)
    public Page<StudySessionResponse> listMine(Long userId, Pageable pageable) {
        Page<StudySession> sessions = repository.findByUserId(userId, pageable);
        List<Long> deckIds = sessions.getContent().stream()
                .map(StudySession::getDeckId).distinct().toList();
        Map<Long, String> titles = deckRepository.findAllById(deckIds).stream()
                .collect(Collectors.toMap(Deck::getId, Deck::getTitle));
        return sessions.map(s -> StudySessionResponse.from(s, titles.get(s.getDeckId())));
    }
}
