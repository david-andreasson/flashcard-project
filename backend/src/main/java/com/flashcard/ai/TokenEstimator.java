package com.flashcard.ai;

/**
 * Rough token estimate (~4 characters per token) for providers whose backend does not report
 * usage. Shared by the mock provider and the 1min.ai provider so the quota and cost pipeline runs
 * on consistent numbers regardless of which provider is active.
 */
public final class TokenEstimator {

    private TokenEstimator() {
    }

    /** ~4 characters per token; at least 1 for any non-empty text, 0 for null or empty. */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
