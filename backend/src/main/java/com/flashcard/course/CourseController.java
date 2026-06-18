package com.flashcard.course;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.PageRequests;
import com.flashcard.common.PagedResponse;
import com.flashcard.course.dto.CourseResponse;
import com.flashcard.course.dto.CreateCourseRequest;
import com.flashcard.course.dto.UpdateCourseRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                 @Valid @RequestBody CreateCourseRequest request) {
        return CourseResponse.from(courseService.create(principal.id(), request.title()));
    }

    @GetMapping("/{id}")
    public CourseResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                              @PathVariable Long id) {
        return CourseResponse.from(courseService.getReadable(id, principal.id()));
    }

    @GetMapping
    public PagedResponse<CourseResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                              @RequestParam(defaultValue = "mine") String scope,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size);
        var result = "public".equalsIgnoreCase(scope)
                ? courseService.listPublic(pageable)
                : courseService.listMine(principal.id(), pageable);
        return PagedResponse.from(result.map(CourseResponse::from));
    }

    @PutMapping("/{id}")
    public CourseResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody UpdateCourseRequest request) {
        return CourseResponse.from(
                courseService.update(id, principal, request.title(), request.visibility()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        courseService.delete(id, principal);
    }
}
