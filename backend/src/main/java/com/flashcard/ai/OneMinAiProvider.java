package com.flashcard.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.flashcard.common.UpstreamAiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Real provider backed by the 1min.ai HTTP API, active when {@code ai.provider=1min}. 1min.ai is a
 * model aggregator: one key and one credit pool reach many models. It accepts a single text prompt
 * and returns generated text only — no token counts — so this provider estimates usage via
 * {@link TokenEstimator}, keeping the quota and logging pipeline unchanged.
 *
 * <p>Contract (the live API rejects the older {@code /api/features} + {@code CHAT_WITH_AI} pair
 * with "Unsupported feature type"; the current unified chat path is used instead):
 * {@code POST {base-url}/api/chat-with-ai} with header {@code API-KEY}, body
 * {@code {type:"UNIFY_CHAT_WITH_AI", model, promptObject:{prompt}}}; the reply carries the text
 * at {@code aiRecord.aiRecordDetail.resultObject}.
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "1min")
public class OneMinAiProvider implements AiProvider {

    private static final String CHAT_PATH = "/api/chat-with-ai";

    private final RestClient restClient;
    private final OneMinProperties properties;

    public OneMinAiProvider(OneMinProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String prompt = buildPrompt(request);
        Map<String, Object> body = Map.of(
                "type", "UNIFY_CHAT_WITH_AI",
                "model", properties.model(),
                "promptObject", Map.of("prompt", prompt));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(CHAT_PATH)
                    .header("API-KEY", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new UpstreamAiException("1min.ai request failed: " + ex.getMessage(), ex);
        }

        String content = extractContent(response);
        int inputTokens = TokenEstimator.estimate(prompt);
        int outputTokens = TokenEstimator.estimate(content);
        return new AiResponse(content, inputTokens, outputTokens, properties.model());
    }

    private static String buildPrompt(AiRequest request) {
        String system = request.systemPrompt() == null ? "" : request.systemPrompt();
        String user = request.userMessage() == null ? "" : request.userMessage();
        return system.isBlank() ? user : system + "\n\n" + user;
    }

    /**
     * Reads {@code aiRecord.aiRecordDetail.resultObject}, which 1min.ai returns either as a text
     * node or as a one-element array of text. Anything else is treated as an upstream failure.
     */
    private static String extractContent(JsonNode response) {
        if (response == null) {
            throw new UpstreamAiException("1min.ai returned an empty response");
        }
        JsonNode result = response.path("aiRecord").path("aiRecordDetail").path("resultObject");
        if (result.isArray() && !result.isEmpty()) {
            return result.get(0).asText();
        }
        if (result.isTextual()) {
            return result.asText();
        }
        throw new UpstreamAiException("1min.ai response did not contain result text");
    }
}
