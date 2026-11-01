package com.scytalys.trackpicture.application;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackUpdateCache {

    private final ConcurrentHashMap<String, TrackUpdate> latestUpdates = new ConcurrentHashMap<>();

    public TrackUpdate get(String key) {
        return latestUpdates.get(key);
    }

    public void update(TrackUpdate update) {
        latestUpdates.compute(update.trackId(), (_, current) ->
                current == null || update.timestamp().isAfter(current.timestamp())
                        ? update
                        : current
        );
    }
}
