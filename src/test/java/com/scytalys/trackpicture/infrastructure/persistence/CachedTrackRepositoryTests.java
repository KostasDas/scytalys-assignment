package com.scytalys.trackpicture.infrastructure.persistence;

import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.application.TrackUpdateCache;
import com.scytalys.trackpicture.message.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachedTrackRepositoryTests {

    @Mock
    private TrackUpdateCache cache;

    @Mock
    private TrackRepository repository;

    private CachedTrackRepository cachedRepository;

    @BeforeEach
    void setUp() {
        cachedRepository = new CachedTrackRepository(cache, repository);
    }

    @Test
    void returnsCacheHitWithoutQueryingDatabase() {
        TrackUpdate update = update();
        when(cache.get(update.trackId())).thenReturn(update);

        Optional<TrackUpdate> result = cachedRepository.findLatestByTrackId(update.trackId());

        assertThat(result).contains(update);
        verifyNoInteractions(repository);
    }

    @Test
    void loadsCacheMissFromDatabaseAndPopulatesCache() {
        TrackUpdate update = update();
        when(cache.get(update.trackId())).thenReturn(null);
        when(repository.findLatestByTrackId(update.trackId())).thenReturn(Optional.of(update));

        Optional<TrackUpdate> result = cachedRepository.findLatestByTrackId(update.trackId());

        assertThat(result).contains(update);
        verify(cache).update(update);
    }

    @Test
    void returnsEmptyWithoutPopulatingCacheWhenTrackDoesNotExist() {
        String trackId = "missing";
        when(cache.get(trackId)).thenReturn(null);
        when(repository.findLatestByTrackId(trackId)).thenReturn(Optional.empty());

        Optional<TrackUpdate> result = cachedRepository.findLatestByTrackId(trackId);

        assertThat(result).isEmpty();
        verify(cache, never()).update(any());
    }

    private static TrackUpdate update() {
        return new TrackUpdate(
                "aircraft-7",
                Instant.parse("2026-11-01T00:00:00Z"),
                35.0,
                24.0,
                8000.0,
                90.0,
                Identity.UNKNOWN
        );
    }
}
