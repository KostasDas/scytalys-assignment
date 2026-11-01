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

I elaborate more on [Decisions](DECISIONS.md) but as a design philosophy, based on the assignment I decided that the following criteria are the most important:
- Correctness
- Reliability
- Simplicity

Simplicity offers us debugging, predictive message handling and is tied to correctness. It is easy to get lost in distributed systems. 
I decided to design for scalability but not scale if it's not needed. Furthermore, it keeps updates atomic and performant. By having one table where we insert the updates
and this table is used to recreate picture and history we essentially reduce surface area for errors to the minimum and ensure state cannot be corrupted. If we had multiple tables (let's say one for track updates one for picture)
we'd need to handle the case where inserting an update succeeds on track updates but fails on picture. A picture or track update read would possibly give us wrong data in that case.

Junk data offer us nothing. It's more important to have the correct data with smaller volume than wrong data. There's one trade off I'm not too sure about:
/picture does not use cache to ensure correctness as DB is source of truth, but I have identified it as an endpoint whose performance is sensitive. 

- Validation: invalid updates are logged and never reach PostgreSQL or the cache.
- Duplicates and replay: (track_id, observed_at) is the database key, so reprocessing the same update cannot duplicate history.
- Ordering: older updates remain in history but cannot replace a newer cached state; /picture selects the latest database row.
- Single partition: guarantees ordered processing for the assignment, sacrificing parallel consumption.


## Stretch goals implemented:

- Invalid updates and malformed JSON are routed to `track-updates-dlt`; validation failures are also logged.

## What I would improve with more time

- Performance improvements:
- - Multiple kafka partitions and more consumers to handle more updates
- - Introduce persistent cache for timing sensitive operations (Get latest location of specific airborne object very fast)
- Identity handling
- - Handle objects differently based on their identity. For example based on what we are doing we would drop friendly and neutral and keep unknown and hostile or the opposite. 
