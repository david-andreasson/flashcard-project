package com.flashcard.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    @Test
    void nullOrEmpty_isZero() {
        assertThat(TokenEstimator.estimate(null)).isZero();
        assertThat(TokenEstimator.estimate("")).isZero();
    }

    @Test
    void nonEmpty_isAtLeastOne_andRoughlyAQuarterOfTheLength() {
        assertThat(TokenEstimator.estimate("a")).isEqualTo(1);
        assertThat(TokenEstimator.estimate("12345678")).isEqualTo(2);      // 8 / 4
        assertThat(TokenEstimator.estimate("x".repeat(100))).isEqualTo(25); // 100 / 4
    }
}
