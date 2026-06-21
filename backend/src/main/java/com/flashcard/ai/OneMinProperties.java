package com.flashcard.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-scoped configuration for the 1min.ai provider. Keeping the model and credentials here
 * (not in the shared {@code ai.*} block) means each provider owns its own settings and the model
 * is chosen per provider. The API key comes from the environment; nothing is hard-coded.
 */
@ConfigurationProperties(prefix = "ai.onemin")
public record OneMinProperties(String baseUrl, String apiKey, String model) {
}
