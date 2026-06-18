package com.flashcard.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Owner-scoped lookup for writes. */
    Optional<Course> findByIdAndOwnerId(Long id, Long ownerId);

    /** Readable if the requester owns it OR it is public. */
    @Query("""
            select c from Course c
            where c.id = :id and (c.ownerId = :requesterId or c.visibility = :publicVisibility)
            """)
    Optional<Course> findReadable(@Param("id") Long id,
                                  @Param("requesterId") Long requesterId,
                                  @Param("publicVisibility") Visibility publicVisibility);

    Page<Course> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Course> findByVisibility(Visibility visibility, Pageable pageable);

    boolean existsByOwnerIdAndTitle(Long ownerId, String title);
}
