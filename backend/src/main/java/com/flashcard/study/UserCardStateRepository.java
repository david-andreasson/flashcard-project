package com.flashcard.study;

import com.flashcard.card.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserCardStateRepository extends JpaRepository<UserCardState, Long> {

    Optional<UserCardState> findByUserIdAndCardId(Long userId, Long cardId);

    // ── progress counts ──
    long countByUserId(Long userId);

    long countByUserIdAndDueAtLessThanEqual(Long userId, Instant now);

    long countByUserIdAndLastReviewedAtGreaterThanEqual(Long userId, Instant start);

    /**
     * A deck's cards that are due for the user: new cards (no state row) or cards whose due date
     * has passed. New cards sort first (null due date), then by due date, then authored order.
     * Uses an entity join because Card and UserCardState have no mapped association.
     */
    @Query("""
            select c from Card c
            left join UserCardState s on s.cardId = c.id and s.userId = :userId
            where c.deckId = :deckId and (s.id is null or s.dueAt <= :now)
            order by s.dueAt asc nulls first, c.createdAt asc
            """)
    List<Card> findDueCards(@Param("userId") Long userId, @Param("deckId") Long deckId, @Param("now") Instant now);
}
