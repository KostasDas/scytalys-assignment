package com.scytalys.trackpicture.infrastructure.web;

import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.infrastructure.persistence.CachedTrackRepository;
import com.scytalys.trackpicture.infrastructure.persistence.SortDirection;
import com.scytalys.trackpicture.infrastructure.persistence.TrackRepository;
import com.scytalys.trackpicture.message.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackControllerTests {

    @Mock
    private TrackRepository repository;

    @Mock
    private CachedTrackRepository cachedRepository;

    private TrackController controller;

    @BeforeEach
    void setUp() {
        controller = new TrackController(repository, cachedRepository);
    }

    @Test
    void returnsLatestTrackWhenPresent() {
        TrackUpdate update = update();
        when(cachedRepository.findLatestByTrackId(update.trackId())).thenReturn(Optional.of(update));

        ResponseEntity<TrackUpdate> response = controller.getTrack(update.trackId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(update);
    }

    @Test
    void returnsNotFoundWhenLatestTrackIsAbsent() {
        String trackId = "missing";
        when(cachedRepository.findLatestByTrackId(trackId)).thenReturn(Optional.empty());

        ResponseEntity<TrackUpdate> response = controller.getTrack(trackId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void returnsHistoryInRequestedDirection() {
        TrackUpdate update = update();
        when(repository.findHistoryByTrackId(update.trackId(), SortDirection.ASC))
                .thenReturn(List.of(update));

        List<TrackUpdate> result = controller.getHistory(update.trackId(), SortDirection.ASC);

        assertThat(result).containsExactly(update);
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
