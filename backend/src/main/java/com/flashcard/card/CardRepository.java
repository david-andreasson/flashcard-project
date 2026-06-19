package com.flashcard.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByDeckId(Long deckId, Pageable pageable);

    Optional<Card> findByIdAndDeckId(Long id, Long deckId);

    boolean existsByDeckIdAndFront(Long deckId, String front);
}
