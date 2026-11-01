package com.scytalys.trackpicture.application;

import com.scytalys.trackpicture.message.Identity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TrackStalenessEvaluatorTests {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    private final TrackStalenessEvaluator evaluator = new TrackStalenessEvaluator(
            300_000,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void marksTrackStaleWhenLatestObservationIsOlderThanThreshold() {
        TrackUpdate update = updateAt("2026-11-01T11:54:59Z");

        assertThat(evaluator.statusOf(update)).isEqualTo(TrackStatus.STALE);
    }

    @Test
    void keepsTrackActiveAtAndWithinThreshold() {
        assertThat(evaluator.statusOf(updateAt("2026-11-01T11:55:00Z")))
                .isEqualTo(TrackStatus.ACTIVE);
        assertThat(evaluator.statusOf(updateAt("2026-11-01T12:00:00Z")))
                .isEqualTo(TrackStatus.ACTIVE);
    }

    private static TrackUpdate updateAt(String timestamp) {
        return new TrackUpdate(
                "aircraft-7",
                Instant.parse(timestamp),
                35.0,
                24.0,
                8000.0,
                90.0,
                Identity.UNKNOWN
        );
    }
}
