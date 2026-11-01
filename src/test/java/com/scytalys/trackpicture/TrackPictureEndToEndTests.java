package com.scytalys.trackpicture;

import com.scytalys.trackpicture.message.Identity;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = "track-updates")
class TrackPictureEndToEndTests {

    @Autowired
    private KafkaTemplate<String, TrackUpdateMessage> kafkaTemplate;

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
}
