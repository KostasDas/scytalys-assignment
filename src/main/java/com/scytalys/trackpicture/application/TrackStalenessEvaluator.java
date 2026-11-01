package com.scytalys.trackpicture.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
public class TrackStalenessEvaluator {

    private final Duration staleAfter;
    private final Clock clock;

    @Autowired
    public TrackStalenessEvaluator(@Value("${TRACK_STALE_AFTER_MS:300000}") long staleAfterMs) {
        this(staleAfterMs, Clock.systemUTC());
    }

    TrackStalenessEvaluator(long staleAfterMs, Clock clock) {
        this.staleAfter = Duration.ofMillis(staleAfterMs);
        this.clock = clock;
    }

    public TrackStatus statusOf(TrackUpdate update) {
        return update.timestamp().isBefore(clock.instant().minus(staleAfter))
                ? TrackStatus.STALE
                : TrackStatus.ACTIVE;
    }
}
