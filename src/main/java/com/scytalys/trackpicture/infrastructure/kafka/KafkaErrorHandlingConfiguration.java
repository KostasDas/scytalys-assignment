package com.scytalys.trackpicture.infrastructure.kafka;

import com.scytalys.trackpicture.application.InvalidTrackUpdateException;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaErrorHandlingConfiguration {

    @Bean
    ProducerFactory<String, TrackUpdateMessage> trackProducerFactory(KafkaProperties properties) {
        JacksonJsonSerializer<TrackUpdateMessage> serializer = new JacksonJsonSerializer<>();
        serializer.noTypeInfo();
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties(),
                new StringSerializer(),
                serializer
        );
    }

    @Bean
    KafkaTemplate<String, TrackUpdateMessage> kafkaTemplate(
            ProducerFactory<String, TrackUpdateMessage> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    ProducerFactory<String, Object> deadLetterProducerFactory(KafkaProperties properties) {
        JacksonJsonSerializer<Object> jsonSerializer = new JacksonJsonSerializer<>();
        jsonSerializer.noTypeInfo();
        DelegatingByTypeSerializer serializer = new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                TrackUpdateMessage.class, jsonSerializer
        ));
        return new DefaultKafkaProducerFactory<>(
                properties.buildProducerProperties(),
                new StringSerializer(),
                serializer
        );
    }

    @Bean
    KafkaTemplate<String, Object> deadLetterKafkaTemplate(
            ProducerFactory<String, Object> deadLetterProducerFactory
    ) {
        return new KafkaTemplate<>(deadLetterProducerFactory);
    }

    @Bean
    DefaultErrorHandler trackUpdateErrorHandler(
            KafkaOperations<String, Object> deadLetterKafkaTemplate,
            @Value("${TRACK_UPDATES_DLT_TOPIC:track-updates-dlt}") String deadLetterTopic
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterKafkaTemplate,
                (_, _) -> new TopicPartition(deadLetterTopic, 0)
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L)
        );
        errorHandler.addNotRetryableExceptions(InvalidTrackUpdateException.class);
        return errorHandler;
    }
}
