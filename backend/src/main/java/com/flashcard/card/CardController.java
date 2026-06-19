package com.flashcard.card;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.dto.CardResponse;
import com.flashcard.card.dto.CreateCardRequest;
import com.flashcard.card.dto.UpdateCardRequest;
import com.flashcard.common.PageRequests;
import com.flashcard.common.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/courses/{courseId}/decks/{deckId}/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long courseId,
                               @PathVariable Long deckId,
                               @Valid @RequestBody CreateCardRequest request) {
        return CardResponse.from(
                cardService.create(courseId, deckId, principal, request.front(), request.back(), request.notes()));
    }

    @GetMapping
    public PagedResponse<CardResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PathVariable Long courseId,
                                            @PathVariable Long deckId,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        // Cards read in authored order (oldest first).
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        var cards = cardService.list(courseId, deckId, principal, pageable);
        return PagedResponse.from(cards.map(CardResponse::from));
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                            @PathVariable Long courseId,
                            @PathVariable Long deckId,
                            @PathVariable Long cardId) {
        return CardResponse.from(cardService.get(courseId, deckId, cardId, principal));
    }

    @PutMapping("/{cardId}")
    public CardResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long courseId,
                               @PathVariable Long deckId,
                               @PathVariable Long cardId,
                               @Valid @RequestBody UpdateCardRequest request) {
        return CardResponse.from(
                cardService.update(courseId, deckId, cardId, principal, request.front(), request.back(), request.notes()));
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthPrincipal principal,
                       @PathVariable Long courseId,
                       @PathVariable Long deckId,
                       @PathVariable Long cardId) {
        cardService.delete(courseId, deckId, cardId, principal);
    }
}
