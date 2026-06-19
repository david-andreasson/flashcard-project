package com.flashcard.deck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    Page<Deck> findByCourseId(Long courseId, Pageable pageable);

    Optional<Deck> findByIdAndCourseId(Long id, Long courseId);

    Optional<Deck> findByCourseIdAndTitle(Long courseId, String title);
}
