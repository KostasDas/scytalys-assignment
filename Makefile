.DEFAULT_GOAL := help

LOCAL_UID := $(shell id -u)
LOCAL_GID := $(shell id -g)
export LOCAL_UID
export LOCAL_GID

COMPOSE := docker compose
PROD_COMPOSE := docker compose -f compose.yaml

.PHONY: help dev prod build test logs down

help:
	@printf '%s\n' \
		'make dev    Start development services with hot reload' \
		'make prod   Start production-style services' \
		'make build  Build the production-style application image' \
		'make test   Run Maven tests in the development container' \
		'make logs   Follow application logs' \
		'make down   Stop services without deleting persistent data'

dev:
	$(COMPOSE) up --build --watch

prod:
	$(PROD_COMPOSE) up --build

build:
	$(PROD_COMPOSE) build app

test:
	$(COMPOSE) run --rm --build app ./mvnw test

logs:
	$(COMPOSE) logs -f app

down:
	$(COMPOSE) down
