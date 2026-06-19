package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.PageRequests;
import com.flashcard.common.PagedResponse;
import com.flashcard.study.dto.RecordSessionRequest;
import com.flashcard.study.dto.StudySessionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse record(@AuthenticationPrincipal AuthPrincipal principal,
                                       @Valid @RequestBody RecordSessionRequest request) {
        return studySessionService.record(
                principal, request.deckId(), request.totalCards(), request.correctCount());
    }

    @GetMapping
    public PagedResponse<StudySessionResponse> listMine(@AuthenticationPrincipal AuthPrincipal principal,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "finishedAt"));
        return PagedResponse.from(studySessionService.listMine(principal.id(), pageable));
    }
}
