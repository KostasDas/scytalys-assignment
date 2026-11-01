package com.scytalys.trackpicture.message;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TrackUpdateMessage(
        @NotBlank String trackId,
        @NotNull Instant timestamp,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @DecimalMin("0.0") Double altitude,
        @DecimalMin("0.0") @DecimalMax(value = "360.0", inclusive = false) Double heading,
        @NotNull Identity identity
) {
}
