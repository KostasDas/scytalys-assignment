package com.scytalys.trackpicture.infrastructure.web;

import com.scytalys.trackpicture.application.TrackStalenessEvaluator;
import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.infrastructure.persistence.CachedTrackRepository;
import com.scytalys.trackpicture.infrastructure.persistence.SortDirection;
import com.scytalys.trackpicture.infrastructure.persistence.TrackRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TrackController {

    private final TrackRepository repository;
    private final CachedTrackRepository cachedRepository;
    private final TrackStalenessEvaluator stalenessEvaluator;

    public TrackController(
            TrackRepository repository,
            CachedTrackRepository cachedRepository,
            TrackStalenessEvaluator stalenessEvaluator
    ) {
        this.repository = repository;
        this.cachedRepository = cachedRepository;
        this.stalenessEvaluator = stalenessEvaluator;
    }

    @GetMapping("/picture")
    public List<TrackUpdateResponse> getPicture() {
        return cachedRepository.findAllLatest().stream()
                .map(update -> TrackUpdateResponse.from(update, stalenessEvaluator.statusOf(update)))
                .toList();
    }

    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<TrackUpdateResponse> getTrack(@PathVariable("trackId") String trackId) {
        return cachedRepository.findLatestByTrackId(trackId)
                .map(update -> TrackUpdateResponse.from(update, stalenessEvaluator.statusOf(update)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tracks/{trackId}/history")
    public List<TrackUpdate> getHistory(
            @PathVariable("trackId") String trackId,
            @RequestParam(defaultValue = "DESC") SortDirection sort
    ) {
        return repository.findHistoryByTrackId(trackId, sort);
    }
}
