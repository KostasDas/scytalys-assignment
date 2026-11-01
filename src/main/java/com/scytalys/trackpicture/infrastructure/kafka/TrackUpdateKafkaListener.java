package com.scytalys.trackpicture.infrastructure.kafka;

import com.scytalys.trackpicture.application.TrackUpdateProcessor;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TrackUpdateKafkaListener {

    private final TrackUpdateProcessor processor;

    public TrackUpdateKafkaListener(TrackUpdateProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(topics = "${TRACK_UPDATES_TOPIC:track-updates}")
    public void handle(TrackUpdateMessage message) {
        processor.process(message);
    }
}
