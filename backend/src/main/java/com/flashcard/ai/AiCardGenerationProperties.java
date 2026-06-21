package com.flashcard.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the AI card-generation feature: the upper bound on cards per request. */
@ConfigurationProperties(prefix = "ai.card-generation")
public record AiCardGenerationProperties(int maxCount) {
}
