package com.flashcard.deck;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.PageRequests;
import com.flashcard.common.PagedResponse;
import com.flashcard.deck.dto.CreateDeckRequest;
import com.flashcard.deck.dto.DeckResponse;
import com.flashcard.deck.dto.UpdateDeckRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/courses/{courseId}/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long courseId,
                               @Valid @RequestBody CreateDeckRequest request) {
        return DeckResponse.from(deckService.create(courseId, principal, request.title()));
    }

    @GetMapping
    public PagedResponse<DeckResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PathVariable Long courseId,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        var decks = deckService.list(courseId, principal, PageRequests.of(page, size));
        return PagedResponse.from(decks.map(DeckResponse::from));
    }

    @GetMapping("/{deckId}")
    public DeckResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                            @PathVariable Long courseId,
                            @PathVariable Long deckId) {
        return DeckResponse.from(deckService.get(courseId, deckId, principal));
    }

    @PutMapping("/{deckId}")
    public DeckResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long courseId,
                               @PathVariable Long deckId,
                               @Valid @RequestBody UpdateDeckRequest request) {
        return DeckResponse.from(deckService.update(courseId, deckId, principal, request.title()));
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal,
                       @PathVariable Long courseId,
                       @PathVariable Long deckId) {
        deckService.delete(courseId, deckId, principal);
    }
}
