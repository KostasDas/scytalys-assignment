# Decisions

## Application and runtime

- **Java 27 and Spring boot:** Normally I'd go for a Java lts release but for a single purpose assignment it doesn't matter much. I chose java and spring boot because it aligns with the company's stack to make it easier to review plus it bootstraps all our requirements.
- **Docker Compose:** we provide a reproducible local environment with little setup, accepting that this single-host configuration is not a production orchestration or high-availability solution.
- **PostgreSQL:** Simple general purpose scalable solution. For this assignment other databases would be just as acceptable since we need atomic inserts and idempotency. (ON CONFLICT DO NOTHING for psql). 
- **Direct JDBC:** ORM would be an overkill plus I'm not a big fan of them, I've encountered many issues, performance and transaction related. I like to keep SQL explicit with no side effects.
- **Single-update inserts rather than batching:** each message is an independent unit and I could not see a solid way of handling batch failures, so I decided to avoid batch transactions.
- **Database exceptions propagate to the messaging boundary:** we do not catch and consume persistence failures in `TrackUpdateProcessor`, allowing Spring Kafka error handling to retry rather than acknowledging updates that were not stored.
- **Database-backed whole picture:** `GET /picture` queries the latest PostgreSQL row per track on every request, favoring correctness over the speed of a potentially incomplete or stale cache; only single-track reads use the in-memory cache.
- **Singleton, concurrency-safe in-memory cache:** one `TrackUpdateCache` is shared safely by consumer and HTTP threads within a single application instance; additional instances would require a shared cache, cache invalidation, or database reads because their local caches are not shared.

## Kafka

Philosophy: I decided we should go for simplicity. Kept everything to a minimum. 1 broker, 1 partition, 1 listener. It satisfies all requirements. In a real-world scenario we'd have to do some math,
to see how we should scale, vertically or horizontally or both. We'd also use trackID as message key for kafka partitions to ensure same tracks are delivered to the same consumers. We'd also encrypt the message communication.

- **Single Kafka process (`KAFKA_NODE_ID=1`):** we use one node to minimize resource use and configuration, accepting that there is no broker failover.
- **Combined broker and controller roles (`KAFKA_PROCESS_ROLES=broker,controller`):** the same process stores messages and manages cluster metadata, which is sufficient for one local node but would be separated or replicated for a resilient deployment.
- **Listener-to-security mapping (`KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT`):** both listeners explicitly use unencrypted and unauthenticated communication because Kafka is accessible only within the local Compose network; a shared deployment would use appropriate `SSL` or `SASL_SSL` settings and credentials.
- **No initial consumer-group rebalance delay (`KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0`):** consumers receive their partition assignments without an artificial startup delay, which is useful for fast local startup.
- **Required topic name (`TRACK_UPDATES_TOPIC=track-updates`):** we use the topic name specified by the assignment and keep it configurable for other deployments.
- **One topic partition (`TRACK_UPDATES_PARTITIONS=1`):** all updates enter one ordered log, simplifying reasoning about ordering and replay while giving up parallel consumption across partitions.
- **Future partitioned scaling:** we could add partitions to process different tracks in parallel, using `trackId` as the message key so every update for a given track is assigned to the same partition and retains per-track arrival ordering.
- **Persistent Kafka storage (`kafka-data`):** messages and cluster metadata survive container restarts, although a single local volume provides neither replication nor backups.
- **One application consumer group (`spring.kafka.consumer.group-id=track-picture`):** application instances share partition assignments and consumer progress; with one partition, only one instance in this group actively consumes at a time.

