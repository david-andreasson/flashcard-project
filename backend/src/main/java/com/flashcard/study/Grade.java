package com.flashcard.study;

/**
 * A user's recall grade on review, mapped to an SM-2 quality score. {@code AGAIN} (quality &lt; 3)
 * is a lapse; the rest are passes of increasing strength.
 */
public enum Grade {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int quality;

    Grade(int quality) {
        this.quality = quality;
    }

    public int quality() {
        return quality;
    }
}
