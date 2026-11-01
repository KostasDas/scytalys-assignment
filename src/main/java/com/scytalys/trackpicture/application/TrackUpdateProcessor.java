package com.scytalys.trackpicture.application;

import com.scytalys.trackpicture.infrastructure.persistence.TrackRepository;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TrackUpdateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TrackUpdateProcessor.class);

    private final Validator validator;
    private final TrackRepository repository;
    private final TrackUpdateCache cache;

    public TrackUpdateProcessor(Validator validator, TrackRepository repository, TrackUpdateCache cache) {
        this.validator = validator;
        this.repository = repository;
        this.cache = cache;
    }

    public void process(TrackUpdateMessage message) {
        Set<ConstraintViolation<TrackUpdateMessage>> violations = validator.validate(message);

        if (!violations.isEmpty()) {
            String reason = String.join("; ", violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .toList());
            logger.error("Rejected update: {}", reason);
            throw new InvalidTrackUpdateException("Rejected update: " + reason);
        }
        logger.info("Processing update {}", message.trackId());
        int inserted = repository.insert(message);
        cache.update(toTrackUpdate(message));

        if (inserted == 0 ) {
            logger.info("Update {} was ignored, already exists", message.trackId());
            return;
        }
        logger.info("Inserted update {}", message.trackId());

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
