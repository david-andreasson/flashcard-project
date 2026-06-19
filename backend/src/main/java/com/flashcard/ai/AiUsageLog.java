package com.flashcard.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** One AI call's usage: who, which feature, how many tokens, and the estimated cost. */
@Entity
@Table(name = "ai_usage_log")
@Getter
@Setter
@NoArgsConstructor
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature_key", nullable = false, length = 50)
    private String featureKey;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AiUsageLog(Long userId, String featureKey, int inputTokens, int outputTokens,
                      BigDecimal estimatedCostUsd) {
        this.userId = userId;
        this.featureKey = featureKey;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.estimatedCostUsd = estimatedCostUsd;
    }
}
