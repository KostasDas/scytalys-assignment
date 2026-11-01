package com.scytalys.trackpicture.application;

import com.scytalys.trackpicture.infrastructure.persistence.TrackRepository;
import com.scytalys.trackpicture.message.Identity;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackUpdateProcessorTests {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private TrackRepository repository;

    @Mock
    private TrackUpdateCache cache;

    private TrackUpdateProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TrackUpdateProcessor(VALIDATOR, repository, cache);
    }

    @Test
    void insertsValidUpdateBeforeUpdatingCache() {
        TrackUpdateMessage message = validMessage();
        when(repository.insert(message)).thenReturn(1);

        processor.process(message);

        InOrder order = inOrder(repository, cache);
        order.verify(repository).insert(message);
        order.verify(cache).update(toTrackUpdate(message));
    }

    @Test
    void acceptsOptionalFieldsAndBoundaryValues() {
        TrackUpdateMessage message = new TrackUpdateMessage(
                "boundary-track",
                Instant.parse("2026-11-01T00:00:00Z"),
                -90.0,
                180.0,
                0.0,
                0.0,
                Identity.UNKNOWN
        );
        TrackUpdateMessage withoutOptionalFields = new TrackUpdateMessage(
                "optional-track",
                Instant.parse("2026-11-01T00:00:00Z"),
                90.0,
                -180.0,
                null,
                null,
                Identity.NEUTRAL
        );

        processor.process(message);
        processor.process(withoutOptionalFields);

        verify(repository).insert(message);
        verify(repository).insert(withoutOptionalFields);
        verify(cache).update(toTrackUpdate(message));
        verify(cache).update(toTrackUpdate(withoutOptionalFields));
    }

    @Test
    void updatesCacheWhenInsertIsDuplicate() {
        TrackUpdateMessage message = validMessage();
        when(repository.insert(message)).thenReturn(0);

        processor.process(message);

        verify(cache).update(toTrackUpdate(message));
    }

    @Test
    void propagatesDatabaseFailureWithoutUpdatingCache() {
        TrackUpdateMessage message = validMessage();
        when(repository.insert(message)).thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThatThrownBy(() -> processor.process(message))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("Database unavailable");

        verifyNoInteractions(cache);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void rejectsInvalidUpdateWithoutWritingAnywhere(TrackUpdateMessage message) {
        processor.process(message);

        verifyNoInteractions(repository, cache);
    }

    private static Stream<TrackUpdateMessage> invalidUpdates() {
        return Stream.of(
                message(" ", Instant.now(), 35.0, 24.0, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", null, 35.0, 24.0, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), null, 24.0, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 90.1, 24.0, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), -90.1, 24.0, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, null, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, 180.1, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, -180.1, 8000.0, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, 24.0, -0.1, 90.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, 24.0, 8000.0, -0.1, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, 24.0, 8000.0, 360.0, Identity.UNKNOWN),
                message("track", Instant.now(), 35.0, 24.0, 8000.0, 90.0, null)
        );
    }

    private static TrackUpdateMessage validMessage() {
        return message(
                "aircraft-7",
                Instant.parse("2026-11-01T00:00:00Z"),
                35.0,
                24.0,
                8000.0,
                90.0,
                Identity.FRIEND
        );
    }

    private static TrackUpdateMessage message(
            String trackId,
            Instant timestamp,
            Double latitude,
            Double longitude,
            Double altitude,
            Double heading,
            Identity identity
    ) {
        return new TrackUpdateMessage(trackId, timestamp, latitude, longitude, altitude, heading, identity);
    }

    private static TrackUpdate toTrackUpdate(TrackUpdateMessage message) {
        return new TrackUpdate(
                message.trackId(),
                message.timestamp(),
                message.latitude(),
                message.longitude(),
                message.altitude(),
                message.heading(),
                message.identity()
        );
    }
}
