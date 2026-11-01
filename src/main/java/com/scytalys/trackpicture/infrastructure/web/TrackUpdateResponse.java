package com.scytalys.trackpicture.infrastructure.web;

import com.scytalys.trackpicture.application.TrackStatus;
import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.message.Identity;

import java.time.Instant;

public record TrackUpdateResponse(
        String trackId,
        Instant timestamp,
        Double latitude,
        Double longitude,
        Double altitude,
        Double heading,
        Identity identity,
        TrackStatus status
) {
    public static TrackUpdateResponse from(TrackUpdate update, TrackStatus status) {
        return new TrackUpdateResponse(
                update.trackId(),
                update.timestamp(),
                update.latitude(),
                update.longitude(),
                update.altitude(),
                update.heading(),
                update.identity(),
                status
        );
    }
}
