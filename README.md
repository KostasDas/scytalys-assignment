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
  TrackUpdateProcessor -- invalid --> rejection log
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

## Key design decisions and trade-offs

TODO

## What I would improve with more time

TODO
