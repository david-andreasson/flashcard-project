package com.flashcard.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByDeckId(Long deckId, Pageable pageable);

    Optional<Card> findByIdAndDeckId(Long id, Long deckId);

    boolean existsByDeckIdAndFront(Long deckId, String front);

    /** Card count grouped by course id (via the card's deck). Each row is [courseId, count]. */
    @Query("select d.courseId, count(c.id) from Card c, Deck d where c.deckId = d.id and d.courseId in :courseIds group by d.courseId")
    List<Object[]> countByCourseIds(@Param("courseIds") List<Long> courseIds);
}
