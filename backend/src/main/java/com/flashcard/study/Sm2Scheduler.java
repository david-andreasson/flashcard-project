package com.flashcard.study;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pure SM-2 scheduler: given a card's current state, a grade, and the current time, it returns the
 * next schedule. No persistence — so it is unit-testable in isolation.
 *
 * <p>Ease updates on every grade and is floored at {@value #MIN_EASE}. A lapse ({@code AGAIN},
 * quality &lt; 3) resets repetitions and the interval to 1 day. A pass grows the interval:
 * 1 day on the first success, 6 on the second, then {@code round(previousInterval * ease)}.
 */
public final class Sm2Scheduler {

    private static final double MIN_EASE = 1.3;

    private Sm2Scheduler() {
    }

    /** The next schedule produced by a review. */
    public record Schedule(double easeFactor, int intervalDays, int repetitions, Instant dueAt) {
    }

    public static Schedule next(double easeFactor, int intervalDays, int repetitions, Grade grade, Instant now) {
        int q = grade.quality();

        double ease = easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (ease < MIN_EASE) {
            ease = MIN_EASE;
        }

        int reps;
        int interval;
        if (q < 3) { // lapse
            reps = 0;
            interval = 1;
        } else {
            reps = repetitions + 1;
            if (reps == 1) {
                interval = 1;
            } else if (reps == 2) {
                interval = 6;
            } else {
                interval = (int) Math.round(intervalDays * ease);
            }
        }

        Instant dueAt = now.plus(interval, ChronoUnit.DAYS);
        return new Schedule(ease, interval, reps, dueAt);
    }
}
