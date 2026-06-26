package com.flashcard.deck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    Page<Deck> findByCourseId(Long courseId, Pageable pageable);

    Optional<Deck> findByIdAndCourseId(Long id, Long courseId);

    Optional<Deck> findByCourseIdAndTitle(Long courseId, String title);

    /** Deck count grouped by course id, for list rendering. Each row is [courseId, count]. */
    @Query("select d.courseId, count(d.id) from Deck d where d.courseId in :courseIds group by d.courseId")
    List<Object[]> countByCourseIds(@Param("courseIds") List<Long> courseIds);
}
