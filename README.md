# Track Picture Service

Spring Boot application foundation with Kafka and PostgreSQL. The track-processing features will be added separately.

## Prerequisites

- Docker with the Docker Compose plugin
- GNU Make

Java and Maven do not need to be installed on the host. Compilation and tests run in containers.

## Configuration

Create your local environment file before starting any services:

```sh
cp .env.example .env
```

Set `POSTGRES_PASSWORD` in `.env` to a long, random value. The `.env` file is excluded from Git and Docker builds; `.env.example` contains only template values and can be committed.

If the PostgreSQL volume was initialized previously, use its existing password. Changing `POSTGRES_PASSWORD` initializes new database containers but does not rotate the password of an existing database.

For a real deployment, supply secrets through the deployment platform's secret manager rather than a local environment file.

## Commands

Run `make help` to list the common commands.

### Development with hot reload

```sh
make dev
```

Development uses `compose.yaml` and `compose.override.yaml`. Source changes are synchronized into the application container and compiled there. Spring Boot DevTools restarts the application without rebuilding the Docker image.

The application listens on `http://localhost:8080` by default. The host port can be changed with `APP_PORT` in `.env`. No track API endpoints are implemented yet.

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

`make down` stops the services without deleting their named volumes. Kafka and PostgreSQL data therefore remain available for subsequent starts.
