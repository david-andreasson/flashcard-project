package com.flashcard.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcard.ai.dto.CardDraft;
import com.flashcard.ai.dto.GenerateCardsResponse;
import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.UpstreamAiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Turns a block of text into flashcard drafts. It owns the prompt (which instructs strict JSON) and
 * the parsing; the cost protection — plan gate, input limit, quota, logging — comes from
 * {@link AiService}. Nothing is persisted here: drafts are saved only when the user creates cards.
 *
 * <p>Not transactional on purpose. {@link AiService#complete} commits the usage log on a successful
 * provider call; a later parse failure here must NOT roll that back, because the tokens were spent.
 */
@Service
public class AiCardGenerationService {

    static final String FEATURE_KEY = "card-generation";

    private final AiService aiService;
    private final AiCardGenerationProperties properties;
    private final ObjectMapper objectMapper;

    public AiCardGenerationService(AiService aiService,
                                   AiCardGenerationProperties properties,
                                   ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public GenerateCardsResponse generate(AuthPrincipal principal, String text, Integer requestedCount) {
        int count = effectiveCount(requestedCount);
        Integer maxTokens = count * 120 + 256;

        AiResponse response = aiService.complete(
                principal, FEATURE_KEY, new AiRequest(buildSystemPrompt(count), text, maxTokens));

        List<CardDraft> drafts = parseDrafts(response.content());
        return new GenerateCardsResponse(
                drafts, response.inputTokens(), response.outputTokens(), response.modelId());
    }

    /** Clamp the requested count to [1, max]; default to the max when unspecified. */
    private int effectiveCount(Integer requested) {
        int max = properties.maxCount();
        if (requested == null) {
            return max;
        }
        return Math.max(1, Math.min(requested, max));
    }

    private static String buildSystemPrompt(int count) {
        return "You create flashcards from the user's text. "
                + "Return ONLY a JSON array of at most " + count + " objects, each with exactly two "
                + "string fields: \"front\" (a question or prompt) and \"back\" (the answer). "
                + "No markdown, no code fences, no commentary — just the JSON array.";
    }

    private List<CardDraft> parseDrafts(String content) {
        String json = extractJsonArray(content);
        try {
            List<CardDraft> drafts = objectMapper.readValue(json, new TypeReference<List<CardDraft>>() {});
            return drafts.stream()
                    .filter(d -> d.front() != null && !d.front().isBlank()
                            && d.back() != null && !d.back().isBlank())
                    .toList();
        } catch (Exception ex) {
            throw new UpstreamAiException("Could not parse card drafts from the AI response", ex);
        }
    }

    /** Tolerate code fences or surrounding prose by extracting the outermost JSON array. */
    private static String extractJsonArray(String content) {
        if (content == null || content.isBlank()) {
            throw new UpstreamAiException("The AI response was empty");
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new UpstreamAiException("The AI response did not contain a JSON array");
        }
        return content.substring(start, end + 1);
    }
}
