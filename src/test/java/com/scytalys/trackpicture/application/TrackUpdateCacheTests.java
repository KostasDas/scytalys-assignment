package com.scytalys.trackpicture.application;

import com.scytalys.trackpicture.message.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TrackUpdateCacheTests {

    private TrackUpdateCache cache;

    @BeforeEach
    void setUp() {
        cache = new TrackUpdateCache();
    }

    @Test
    void returnsNullWhenTrackIsAbsent() {
        assertThat(cache.get("missing")).isNull();
    }

    @Test
    void returnsStoredUpdate() {
        TrackUpdate update = update("aircraft-7", "2026-11-01T00:00:00Z", 35.0);

        cache.update(update);

        assertThat(cache.get(update.trackId())).isEqualTo(update);
    }

    @Test
    void newerUpdateReplacesOlderUpdate() {
        TrackUpdate older = update("aircraft-7", "2026-11-01T00:00:00Z", 35.0);
        TrackUpdate newer = update("aircraft-7", "2026-11-01T01:00:00Z", 36.0);

        cache.update(older);
        cache.update(newer);

        assertThat(cache.get(older.trackId())).isEqualTo(newer);
    }

    @Test
    void olderUpdateDoesNotReplaceNewerUpdate() {
        TrackUpdate newer = update("aircraft-7", "2026-11-01T01:00:00Z", 36.0);
        TrackUpdate older = update("aircraft-7", "2026-11-01T00:00:00Z", 35.0);

        cache.update(newer);
        cache.update(older);

        assertThat(cache.get(newer.trackId())).isEqualTo(newer);
    }

    @Test
    void equalTimestampDoesNotReplaceExistingUpdate() {
        TrackUpdate existing = update("aircraft-7", "2026-11-01T00:00:00Z", 35.0);
        TrackUpdate duplicateWithDifferentPayload = update("aircraft-7", "2026-11-01T00:00:00Z", 99.0);

        cache.update(existing);
        cache.update(duplicateWithDifferentPayload);

        assertThat(cache.get(existing.trackId())).isEqualTo(existing);
    }

    @Test
    void tracksAreUpdatedIndependently() {
        TrackUpdate aircraft = update("aircraft-7", "2026-11-01T00:00:00Z", 35.0);
        TrackUpdate uav = update("uav-1", "2026-11-01T01:00:00Z", 40.0);

        cache.update(aircraft);
        cache.update(uav);

        assertThat(cache.get(aircraft.trackId())).isEqualTo(aircraft);
        assertThat(cache.get(uav.trackId())).isEqualTo(uav);
    }

    private static TrackUpdate update(String trackId, String timestamp, Double latitude) {
        return new TrackUpdate(
                trackId,
                Instant.parse(timestamp),
                latitude,
                24.0,
                8000.0,
                90.0,
                Identity.UNKNOWN
        );
    }
}
