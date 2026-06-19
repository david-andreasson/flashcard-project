package com.flashcard.study;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Page<StudySession> findByUserId(Long userId, Pageable pageable);
}
