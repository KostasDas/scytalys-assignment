package com.scytalys.trackpicture.infrastructure.persistence;

import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.application.TrackUpdateCache;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CachedTrackRepository {

    private final TrackUpdateCache cache;
    private final TrackRepository repository;

    public CachedTrackRepository(TrackUpdateCache cache, TrackRepository repository) {
        this.cache = cache;
        this.repository = repository;
    }

    public Optional<TrackUpdate> findLatestByTrackId(String trackId) {
        TrackUpdate cached = cache.get(trackId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<TrackUpdate> latest = repository.findLatestByTrackId(trackId);
        latest.ifPresent(cache::update);
        return latest;
    }
}
