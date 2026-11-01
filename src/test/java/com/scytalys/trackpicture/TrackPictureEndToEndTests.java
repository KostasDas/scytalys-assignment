package com.scytalys.trackpicture;

import com.scytalys.trackpicture.message.Identity;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"track-updates", "track-updates-dlt"})
class TrackPictureEndToEndTests {

    @Autowired
    private KafkaTemplate<String, TrackUpdateMessage> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void waitForListenerAssignment() {
        listenerRegistry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, 1));
    }

    @Test
    void processesTrackUpdateFromKafkaToReadApi() throws Exception {
        TrackUpdateMessage update = new TrackUpdateMessage(
                "aircraft-7",
                Instant.parse("2026-10-29T12:30:00Z"),
                35.2,
                24.8,
                8200.0,
                91.5,
                Identity.UNKNOWN
        );

        kafkaTemplate.send("track-updates", update.trackId(), update).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/tracks/" + update.trackId()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertThat(response.statusCode()).isEqualTo(200);
            TrackUpdateMessage actual = objectMapper.readValue(response.body(), TrackUpdateMessage.class);
            assertThat(actual).isEqualTo(update);
        });
    }

    @Test
    void routesInvalidAndMalformedUpdatesToDeadLetterTopic() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(embeddedKafka, "dlt-test", false);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        try (Consumer<String, byte[]> consumer =
                     new DefaultKafkaConsumerFactory<String, byte[]>(consumerProperties).createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "track-updates-dlt");

            TrackUpdateMessage invalidUpdate = new TrackUpdateMessage(
                    "invalid-track",
                    Instant.parse("2026-11-01T10:00:00Z"),
                    95.0,
                    24.0,
                    5000.0,
                    90.0,
                    Identity.FRIEND
            );
            byte[] malformedJson = "{\"trackId\":".getBytes(StandardCharsets.UTF_8);

            kafkaTemplate.send("track-updates", invalidUpdate.trackId(), invalidUpdate).get(10, TimeUnit.SECONDS);
            try (KafkaProducer<String, byte[]> rawProducer = new KafkaProducer<>(
                    KafkaTestUtils.producerProps(embeddedKafka),
                    new StringSerializer(),
                    new ByteArraySerializer())) {
                rawProducer.send(new ProducerRecord<>("track-updates", "malformed", malformedJson))
                        .get(10, TimeUnit.SECONDS);
            }

            ConsumerRecord<String, byte[]> invalidRecord = KafkaTestUtils.getSingleRecord(
                    consumer, "track-updates-dlt", Duration.ofSeconds(10));
            assertThat(invalidRecord.key()).isEqualTo("invalid-track");
            assertThat(objectMapper.readValue(invalidRecord.value(), TrackUpdateMessage.class))
                    .isEqualTo(invalidUpdate);

            ConsumerRecord<String, byte[]> malformedRecord = KafkaTestUtils.getSingleRecord(
                    consumer, "track-updates-dlt", Duration.ofSeconds(10));
            assertThat(malformedRecord.key()).isEqualTo("malformed");
            assertThat(malformedRecord.value()).isEqualTo(malformedJson);
        }
    }

}
