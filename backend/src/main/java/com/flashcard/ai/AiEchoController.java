package com.flashcard.ai;

import com.flashcard.ai.dto.EchoRequest;
import com.flashcard.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin demo endpoint that runs the full {@link AiService} pipeline (kill-switch, plan gate,
 * input limit, quota, provider, logging) with the mock provider, so the gates are observable
 * end-to-end. It is the call-site pattern the first real feature (change 07) copies.
 */
@RestController
@RequestMapping("/ai")
public class AiEchoController {

    private final AiService aiService;

    public AiEchoController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/echo")
    public AiResponse echo(@AuthenticationPrincipal AuthPrincipal principal,
                           @Valid @RequestBody EchoRequest request) {
        return aiService.complete(principal, "echo",
                new AiRequest("You are a helpful assistant.", request.message(), null));
    }
}
