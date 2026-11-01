# Track Picture Service

A Spring Boot service that consumes airborne track updates from Kafka, stores their history in PostgreSQL, and exposes the latest picture and track history through HTTP APIs.

## Run

Prerequisites: Docker with Docker Compose and GNU Make. Java and Maven are not required on the host.

```sh
cp .env.example .env
make dev
```

Development mode starts Kafka, PostgreSQL, and the application with hot reload. The API listens on `http://localhost:8080` by default.

In another terminal, publish the demonstration updates:

```sh
make demo-producer
```

The producer publishes three moving tracks, one duplicate, one out-of-order update, and one invalid update.

Try the APIs:

```sh
curl http://localhost:8080/picture
curl http://localhost:8080/tracks/uav-1
curl "http://localhost:8080/tracks/uav-1/history?sort=ASC"
```

Other commands:

```sh
make test   # Run the test suite
make logs   # Follow application logs
make down   # Stop services without deleting persistent data
make prod   # Run the production-style container
make help   # List all commands
```

## Architecture

```text
External source / demo producer
              |
              v
     Kafka: track-updates
              |
              v
 TrackUpdateKafkaListener
              |
              v
  TrackUpdateProcessor -- invalid --> rejection log + dlt
              |
           valid
              |
      +-------+--------+
      |                |
      v                v
PostgreSQL       TrackUpdateCache
(history and      (latest per track)
 latest state)          |
      |                 +----------> GET /tracks/{trackId}
      |                           (with database fallback)
      |
      +----------------> GET /picture 
      |
      +----------------> GET /tracks/{trackId}/history
```
API is exposed through TrackController.

## Key design decisions and trade-offs

**Single durable source of truth:** PostgreSQL stores every accepted update, and both picture and history are derived from it. This avoids inconsistencies between separate picture and history stores, at the cost of more database reads and no independent scaling of the two read models.

**Single consumer group and partition:** this keeps per-track ordering and failure handling simple, while limiting parallel consumption.

**Selective caching:** single-track reads use an in-memory cache for speed. `/picture` queries PostgreSQL directly because caching the whole picture would require a reliable way to detect stale entries; this favors correctness over latency.

**Alternative considered:** separate picture and history consumer groups would allow independent processing and scaling, but would introduce more infrastructure and consistency concerns without improving correctness for this assignment.

More configuration details are in [Decisions](DECISIONS.md).


- Validation: invalid updates are logged and never reach PostgreSQL or the cache.
- Duplicates and replay: (track_id, observed_at) is the database key, so reprocessing the same update cannot duplicate history.
- Ordering: older updates remain in history but cannot replace a newer cached state; /picture selects the latest database row based on timestamp.
- Single partition: guarantees ordered processing for the assignment, sacrificing parallel consumption.

## Key note: alternative architecture

A second valid approach would use separate picture and history consumer groups, with every message delivered to both. 
The picture side would keep the latest state in memory, while the history side would store every unique update. 
It was not chosen because one consumer group covers the assignment with less infrastructure and fewer consistency concerns, while leaving room to introduce the second group later if independent processing or scaling becomes necessary.



## Stretch goals implemented:
- Invalid updates and malformed JSON are routed to `track-updates-dlt`; validation failures are also logged.
- Picture and single-track responses include `ACTIVE` or `STALE`, based on the configurable `TRACK_STALE_AFTER_MS` threshold (five minutes by default).
- "Rebuild the picture from the topic on restart instead of starting empty" this is implemented by default because our picture does not start empty on restart, only the cache does.
- Unit tests included test for validation, cache behaviour and ordering. Application does not test for idempotency in unit tests because I decided to leave database out of them. The demo-producer script demonstrates it though.

## What I would improve with more time

Technical improvements
- Last stretch goal implemented "Metrics or a health endpoint showing consumer lag"
- DLT message handler, redelivery, statistics and diagnostics. Same treatment for logs.
- Performance improvements:
  - Multiple kafka partitions and more consumers to handle more updates. This would require a stress test, see how far we can take the current implementation and decide how to scale.
  - Introduce persistent cache for timing sensitive operations (Get latest location of specific airborne object very fast)
  - Image sliming and container performance improvements: Check if we can make the docker containers smaller also inspect end container for cpu and memory usage during stress test.
- Encrypted communication so that messages cannot be viewed.
- Visualisation of the tracks

Business improvements
- Identity handling
- - Handle objects differently based on their identity. For example based on what we are doing we would drop friendly and neutral and keep unknown and hostile or the opposite. 
- - Enhance the API to allow fetching based on Identity. For example picture of all hostile objects


## AI Disclosure
I used AI for:
- Scaffolding the initial setup before inspecting the docker and docker compose files myself.
- I am familiar with Java but not very familiar with Spring boot, so I asked a lot of questions about the opinionated nature of spring boot.
- Generating code comments.
- The DLT configuration, specifically the KafkaErrorHandlingConfiguration and the compose changes for the dlt
- The demo-producer.sh
- The end to end tests.

I retain ownership of what was written, Any code that was AI generated I have read and inspected, architectural and design decisions were mine.