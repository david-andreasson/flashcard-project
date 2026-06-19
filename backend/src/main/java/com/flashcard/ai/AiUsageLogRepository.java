package com.flashcard.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    /** Total tokens (input + output) a user has consumed since the given instant. */
    @Query("""
            select coalesce(sum(l.inputTokens + l.outputTokens), 0)
            from AiUsageLog l
            where l.userId = :userId and l.createdAt >= :since
            """)
    long sumTokensSince(@Param("userId") Long userId, @Param("since") Instant since);
}
