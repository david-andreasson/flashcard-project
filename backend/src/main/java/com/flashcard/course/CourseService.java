package com.flashcard.course;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.ForbiddenException;
import com.flashcard.common.NotFoundException;
import com.flashcard.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional
    public Course create(Long ownerId, String title) {
        return courseRepository.save(new Course(ownerId, title, Visibility.PRIVATE));
    }

    /** A course the requester may read: their own, or any PUBLIC one. Else 404. */
    @Transactional(readOnly = true)
    public Course getReadable(Long id, Long requesterId) {
        return courseRepository.findReadable(id, requesterId, Visibility.PUBLIC)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    /** A course the requester may write: their own, or any course if ADMIN. Else 404. */
    @Transactional(readOnly = true)
    public Course getWritable(Long id, AuthPrincipal principal) {
        return (principal.role() == Role.ADMIN
                ? courseRepository.findById(id)
                : courseRepository.findByIdAndOwnerId(id, principal.id()))
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    @Transactional(readOnly = true)
    public Page<Course> listMine(Long ownerId, Pageable pageable) {
        return courseRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Course> listPublic(Pageable pageable) {
        return courseRepository.findByVisibility(Visibility.PUBLIC, pageable);
    }

    @Transactional
    public Course update(Long id, AuthPrincipal principal, String title, Visibility visibility) {
        Course course = getWritable(id, principal);
        if (visibility == Visibility.PUBLIC && principal.role() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can make a course public");
        }
        if (title != null) {
            course.setTitle(title);
        }
        if (visibility != null) {
            course.setVisibility(visibility);
        }
        return courseRepository.save(course);
    }

    @Transactional
    public void delete(Long id, AuthPrincipal principal) {
        Course course = getWritable(id, principal);
        courseRepository.delete(course); // decks cascade at the DB level
    }
}
