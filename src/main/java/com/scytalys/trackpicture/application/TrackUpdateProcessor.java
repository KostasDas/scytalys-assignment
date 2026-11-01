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

    public TrackUpdateProcessor(Validator validator, TrackRepository repository) {
        this.validator = validator;
        this.repository = repository;
    }

    public void process(TrackUpdateMessage message) {
        Set<ConstraintViolation<TrackUpdateMessage>> violations = validator.validate(message);

        if (!violations.isEmpty()) {
            violations.forEach(violation -> logger.error(
                    "Rejected message: {} {}",
                    violation.getPropertyPath(),
                    violation.getMessage()
            ));
            return;
        }
        logger.info("Processing message {}", message.trackId());
        int id = repository.insert(message);
        if (id == 0 ) {
            logger.info("Message {} was ignored, already exists", message.trackId());
            return;
        }
        logger.info("Processed message {}, acquired id {}", message.trackId(), id);

    }
}
