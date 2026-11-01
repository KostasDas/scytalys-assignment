package com.scytalys.trackpicture.application;

import com.scytalys.trackpicture.message.Identity;

import java.time.Instant;

public record TrackUpdate(
        String trackId,
        Instant timestamp,
        Double latitude,
        Double longitude,
        Double altitude,
        Double heading,
        Identity identity
) {
}
