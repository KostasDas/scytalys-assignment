# Decisions

## Application and runtime

- **Java 27:** we use the latest Java release for its current capabilities, accepting a shorter support window and possible library or tooling compatibility issues compared with an LTS release.
- **Spring Boot:** we accept additional memory use and framework-managed configuration in exchange for established web, Kafka, database, testing, and development infrastructure.
- **One application with internal modules:** we keep deployment and local development simple, accepting that picture and history components cannot be deployed or scaled independently.
- **Docker Compose:** we provide a reproducible local environment with little setup, accepting that this single-host configuration is not a production orchestration or high-availability solution.
- **Separate development and production-style runtime configurations:** we gain containerized hot reload without rebuilding images, while keeping development tools and source mounts out of the production-style runtime at the cost of maintaining an additional Compose override.
- **PostgreSQL:** we support large histories and concurrent reads and writes, accepting more memory use and operational overhead than an embedded database such as SQLite.
- **Direct JDBC:** we keep database access explicit and avoid learning an object-mapping framework, accepting more manual SQL and result-mapping code.
- **Single-update inserts rather than batching:** each message is an independent unit, so we avoid batch transaction and partial-failure semantics, accepting more database round trips than batch processing.
- **Database exceptions propagate to the messaging boundary:** we do not catch and swallow persistence failures in `TrackUpdateProcessor`, allowing Spring Kafka error handling to retry rather than acknowledging updates that were not stored.
- **Local environment configuration:** we keep credentials and host-specific settings out of source control, accepting that `.env` files are not a production secret-management solution.
- **Persistent PostgreSQL storage:** we preserve database state across container restarts, accepting that a local volume provides neither backups nor disaster recovery.

## Kafka

- **Single Kafka process (`KAFKA_NODE_ID=1`):** we use one node to minimize resource use and configuration, accepting that there is no broker failover.
- **Combined broker and controller roles (`KAFKA_PROCESS_ROLES=broker,controller`):** the same process stores messages and manages cluster metadata, which is sufficient for one local node but would be separated or replicated for a resilient deployment.
- **KRaft rather than ZooKeeper:** Kafka manages its own metadata through its controller, reducing the number of services we need to run.
- **Fixed cluster identity (`KAFKA_CLUSTER_ID`):** the identifier remains stable so the broker can reuse its persistent metadata volume; it is not a credential.
- **Internal application listener (`KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093`):** port 9092 handles producers and consumers, while port 9093 handles controller traffic, separating the two kinds of communication.
- **Advertised Docker address (`KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092`):** clients receive the broker's Compose-network hostname so they can continue communicating with it after their initial connection.
- **Application bootstrap address (`KAFKA_BOOTSTRAP_SERVERS=kafka:9092`):** the application and initialization container use the stable Compose service name rather than a container IP address.
- **Listener-to-security mapping (`KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT`):** both listeners explicitly use unencrypted and unauthenticated communication because Kafka is accessible only within the local Compose network; a shared deployment would use appropriate `SSL` or `SASL_SSL` settings and credentials.
- **Controller listener (`KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER`):** the broker knows which named listener is reserved for cluster-metadata traffic.
- **Single controller voter (`KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093`):** node 1 is the only controller, which keeps the setup simple but means controller failure stops cluster-metadata management.
- **One copy of consumer-offset records (`KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`):** Kafka can store consumer progress on its only broker, accepting loss of that progress if the broker's data is lost.
- **One copy of transaction-state records (`KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1`):** the transaction log can be created on a single broker, without replication.
- **One required in-sync transaction replica (`KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1`):** transaction records require acknowledgement from the only available replica; a higher value would prevent writes on this single-broker setup.
- **No initial consumer-group rebalance delay (`KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0`):** consumers receive their partition assignments without an artificial startup delay, which is useful for fast local startup.
- **Required topic name (`TRACK_UPDATES_TOPIC=track-updates`):** we use the topic name specified by the assignment and keep it configurable for other deployments.
- **One topic partition (`TRACK_UPDATES_PARTITIONS=1`):** all updates enter one ordered log, simplifying reasoning about ordering and replay while giving up parallel consumption across partitions.
- **Explicit topic initialization (`kafka-init`):** a short-lived container creates the topic with `--if-not-exists`, making startup repeatable without failing when the topic already exists.
- **Persistent Kafka storage (`kafka-data`):** messages and cluster metadata survive container restarts, although a single local volume provides neither replication nor backups.
- **No host-published Kafka port:** Kafka is reachable only by containers on the Compose network, reducing accidental external access to its unauthenticated listener.
- **One application consumer group (`spring.kafka.consumer.group-id=track-picture`):** application instances share partition assignments and consumer progress; with one partition, only one instance in this group actively consumes at a time.
- **String message keys:** string deserialization preserves the producer's track identifier key without introducing a second serialization format.
- **Typed JSON message values:** Spring Kafka and Jackson convert JSON directly into `TrackUpdateMessage`, avoiding a manual mapper for this single-message topic while moving malformed-JSON handling into the messaging error boundary we will define later.
- **Fixed value type and restricted type trust:** producer-supplied Java type headers are ignored and only our message package is trusted, reducing the risk of unsafe or unintended polymorphic deserialization.
- **Direct listener-to-processor dispatch:** `@KafkaListener` invokes the messaging adapter, which calls `TrackUpdateProcessor`; no custom dispatcher is needed while the topic carries only one message type.
