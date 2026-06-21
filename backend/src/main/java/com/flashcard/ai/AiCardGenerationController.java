package com.flashcard.ai;

import com.flashcard.ai.dto.GenerateCardsRequest;
import com.flashcard.ai.dto.GenerateCardsResponse;
import com.flashcard.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates flashcard drafts from pasted text. The gates (kill-switch, plan, input limit, quota,
 * logging) are applied by {@link AiService}; this endpoint only shapes the request and response.
 * It persists nothing — saving happens via the cards bulk endpoint.
 */
@RestController
@RequestMapping("/ai/cards")
public class AiCardGenerationController {

    private final AiCardGenerationService generationService;

    public AiCardGenerationController(AiCardGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    public GenerateCardsResponse generate(@AuthenticationPrincipal AuthPrincipal principal,
                                          @Valid @RequestBody GenerateCardsRequest request) {
        return generationService.generate(principal, request.text(), request.count());
    }
}
