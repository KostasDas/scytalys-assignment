# Track Picture Service

Spring Boot application foundation with Kafka and PostgreSQL. The track-processing features will be added separately.

## Prerequisites

- Docker with the Docker Compose plugin
- GNU Make

Java and Maven do not need to be installed on the host. Compilation and tests run in containers.

## Commands

Run `make help` to list the common commands.

### Development with hot reload

```sh
make dev
```

Development uses `compose.yaml` and `compose.override.yaml`. Source changes are synchronized into the application container and compiled there. Spring Boot DevTools restarts the application without rebuilding the Docker image.

The application listens on `http://localhost:8080`. No track API endpoints are implemented yet.

### Production-style runtime

```sh
make prod
```

This uses only `compose.yaml`. The application is packaged into an image containing a Java runtime, without the source tree, Maven, or development tools.

### Other commands

```sh
make test
make logs
make down
```

`make down` stops the services without deleting their named volumes. Kafka and PostgreSQL data therefore remain available for subsequent starts. The database credentials in the Compose file are local demonstration values, not production secrets.
