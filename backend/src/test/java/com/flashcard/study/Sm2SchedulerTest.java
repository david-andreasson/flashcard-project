package com.flashcard.study;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2SchedulerTest {

    private static final Instant NOW = Instant.parse("2026-06-21T00:00:00Z");

    @Test
    void firstSuccess_intervalOneDay_repsOne() {
        Sm2Scheduler.Schedule s = Sm2Scheduler.next(2.5, 0, 0, Grade.GOOD, NOW);
        assertThat(s.repetitions()).isEqualTo(1);
        assertThat(s.intervalDays()).isEqualTo(1);
        assertThat(s.dueAt()).isEqualTo(NOW.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void secondSuccess_intervalSixDays() {
        Sm2Scheduler.Schedule s = Sm2Scheduler.next(2.5, 1, 1, Grade.GOOD, NOW);
        assertThat(s.repetitions()).isEqualTo(2);
        assertThat(s.intervalDays()).isEqualTo(6);
    }

    @Test
    void thirdSuccess_intervalScalesByEase() {
        // round(6 * 2.5) = 15; GOOD (q=4) leaves ease unchanged at 2.5
        Sm2Scheduler.Schedule s = Sm2Scheduler.next(2.5, 6, 2, Grade.GOOD, NOW);
        assertThat(s.repetitions()).isEqualTo(3);
        assertThat(s.intervalDays()).isEqualTo(15);
    }

    @Test
    void again_resetsRepsAndShortens_andLowersEase() {
        Sm2Scheduler.Schedule s = Sm2Scheduler.next(2.5, 30, 5, Grade.AGAIN, NOW);
        assertThat(s.repetitions()).isZero();
        assertThat(s.intervalDays()).isEqualTo(1);
        assertThat(s.easeFactor()).isLessThan(2.5);
    }

    @Test
    void easy_raisesEase() {
        Sm2Scheduler.Schedule s = Sm2Scheduler.next(2.5, 6, 2, Grade.EASY, NOW);
        assertThat(s.easeFactor()).isGreaterThan(2.5);
    }

    @Test
    void ease_neverDropsBelowFloor() {
        double ease = 1.3;
        for (int i = 0; i < 10; i++) {
            ease = Sm2Scheduler.next(ease, 1, 0, Grade.AGAIN, NOW).easeFactor();
        }
        assertThat(ease).isGreaterThanOrEqualTo(1.3);
    }
}
