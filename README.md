# property-management-system

A microservices-based property management system for a housing cooperative: residents,
billing, maintenance requests and reservations of shared resources. Built for the PG3402
Microservices exam.

## Contents

- [Quick start](#quick-start)
- [Demo users and data](#demo-users-and-data)
- [What to try](#what-to-try)
- [Architecture](#architecture)
- [Consul: service discovery, health and configuration](#consul-service-discovery-health-and-configuration)
- [Load balancing and several instances](#load-balancing-and-several-instances)
- [Kafka: asynchronous events](#kafka-asynchronous-events)
- [Running the tests](#running-the-tests)
- [Developing in the IDE](#developing-in-the-ide)
- [More documentation](#more-documentation)

## Quick start

Requires Docker (with Compose). Nothing else needs to be installed.

```bash
cp .env.example .env     # then set JWT_SECRET, BOARD_SEED_PASSWORD and WORKER_SEED_PASSWORD
docker compose up --build
```

Open **http://localhost:3000**. The first start builds all images and takes a few minutes;
the app is ready when all containers are `healthy` (`docker compose ps`).

Notes:

- `JWT_SECRET` must be at least 32 characters. It is shared by all services and the gateway.
- Only the frontend (nginx, port 3000) is published for the application. It serves the React
  app and proxies `/api` to the gateway; the gateway and the services are reachable inside the
  compose network only, so nothing can bypass the JWT check. Consul (8500) and the databases
  (5432-5435) are also published for inspection.
- To start from scratch (empty databases, demo data re-created): `docker compose down -v`.
- Port already in use? Change `FRONTEND_PORT` in `.env`.

## Demo users and data

On first start every service fills its (empty) database with demo data: 6 apartments in
"Solsiden Borettslag" (101-302), 8 residents, invoices for last and this month (one overdue),
7 maintenance requests in all statuses, 3 workers, 5 resources (3 parking spaces, common room,
roof terrace) and a few upcoming bookings. Set `SEED_DEMO_DATA=false` to start with only the
board account and the workers.

| Role | Sign in at | Email | Password |
|---|---|---|---|
| Board member | http://localhost:3000 | `board@example.com` | `BOARD_SEED_PASSWORD` from `.env` |
| Resident | http://localhost:3000 | `kari@example.com` (also `ola@`, `ingrid@`, `lars@`, `sofie@`, `emil@`, `nora@example.com`) | `demo-resident-pw` |
| Maintenance worker | http://localhost:3000/worker/login | `per@example.com` (plumbing, heating), `jon@example.com` (electrical, appliances), `ola@example.com` (all) | `WORKER_SEED_PASSWORD` from `.env` |

Ola and Ingrid Hansen share apartment 201, and Sofie and Emil share 301.

## What to try

**Resident** (e.g. `kari@example.com`)
- *Home / Account*: apartment, household and contact information (editable).
- *Maintenance*: create a request (category, location, priority, description) and follow its status.
- *Invoices*: see the current invoice and history, and simulate paying it.
- *Reservations*: pick a resource, see free one-hour slots, book and cancel.
- *Notifications*: appears when a worker completes one of your requests (see [Kafka](#kafka-asynchronous-events)).

**Board member**
- *Overview*: apartments, occupied/vacant, residents.
- *Residents* and *Apartments*: add, edit, remove; add or move residents from an apartment.
- *Maintenance*: all open requests by priority, assign a worker.
- *Invoices*: generate monthly invoices, see paid/pending/overdue for the building, mark as paid.
- *Resources*: manage shared resources, block one for a period (e.g. maintenance), see its bookings.

**Maintenance worker**
- *Work orders*: your assigned requests. Accept, start and complete them. Completing a request
  publishes the `MaintenanceCompleted` event, and the resident gets a notification.

Suggested end-to-end demo: as `kari@example.com` create a maintenance request, as board assign
it to Per, as Per accept, start and complete it, then check Kari's notifications.

## Architecture

```
browser -> nginx (frontend) :3000 -> gateway :8080 -> resident :8081 | billing :8082
                                                      maintenance :8083 | reservation :8084
```

| Service | Responsibility | Database |
|---|---|---|
| frontend | React app, served by nginx, proxies `/api` | |
| gateway | Single entry point: routing, CORS, JWT check, login rate limit, load balancing | |
| resident-service | Apartments, residents, login, notifications | `resident-db` (5432) |
| billing-service | Monthly invoices, payments, invoice PDF | `billing-db` (5433) |
| maintenance-service | Maintenance requests, workers, work orders | `maintenance-db` (5434) |
| reservation-service | Shared resources and bookings | `reservation-db` (5435) |
| security-common | Shared library (JWT), not a deployable service | |
| consul | Service discovery, health overview, central configuration | |
| kafka | Message broker (KRaft, single node) | |

Each service owns its database (Postgres, schema managed by Flyway) and never reads another's.

Communication:

- **Synchronous:** billing-service calls resident-service over REST (`ResidentServiceClient`,
  with a service token) to get the apartments and the signed-in resident, which it needs to
  generate invoices per apartment and to show each resident their own.
- **Asynchronous:** maintenance-service publishes `MaintenanceCompleted` to Kafka and
  resident-service consumes it ([details](docs/architecture.md)).

Gateway routes (the `/api` prefix is stripped):

| Path | Service |
|---|---|
| `/api/auth`, `/api/residents`, `/api/apartments`, `/api/notifications` | resident-service |
| `/api/invoices` | billing-service |
| `/api/maintenance`, `/api/workers` | maintenance-service |
| `/api/reservations`, `/api/resources` | reservation-service |

The gateway validates the JWT and the services validate it again and enforce roles. Logins are
limited per client address (default 5 per minute, `429` when exceeded; kept in memory, so it is
per gateway instance).

## Consul: service discovery, health and configuration

Consul UI: **http://localhost:8500**

- **Discovery and routing:** every service and the gateway register themselves in Consul. The
  gateway routes with `lb://<service-name>`, so it has no hard-coded service addresses.
- **Health:** the *Services* page shows each instance's health check (`/actuator/health`,
  including database status). Compose uses the same endpoint for start-up order.
- **Central configuration:** the files in [`consul-config/`](consul-config) are loaded into
  Consul's key/value store by the `consul-config` container on every start
  (`config/application/data` for all services, `config/gateway/data` for the gateway). The
  services read them at start-up via `spring.config.import`. Today that is the JWT lifetime,
  the Kafka topic name, the demo-data switch and the login rate limit.
  See them under *Key/Value*; edit the file (or the key) and restart the service to apply a change.
- **Not in Consul on purpose:** secrets (`JWT_SECRET`, database passwords) stay in `.env` because
  Consul's dev mode is unauthenticated and unencrypted. Per-environment addresses (database
  URLs, Kafka address) stay in environment variables too.

If Consul is unavailable a service still starts (`optional:consul:`), using its built-in defaults.

## Load balancing and several instances

Run two copies of the reservation service; the gateway spreads requests over them:

```bash
docker compose up -d --scale reservation-service=2
```

- Both instances appear under *Services -> reservation-service* in Consul.
- Watch the requests alternate: `docker compose logs -f reservation-service`
  (the prefix shows `reservation-service-1` / `-2`), then use the Reservations page.
- Stop one (`docker stop <container>`): after a few seconds only healthy instances receive traffic.
- Booking is safe with several instances: the database refuses overlapping active reservations
  for the same resource (an exclusion constraint), so two simultaneous requests for the same slot
  give one success and one `409`.

Limits: reservation is the service prepared for this. The login rate limit is kept in memory per
gateway instance, so run one gateway.

## Kafka: asynchronous events

When a worker completes a request, maintenance-service publishes `MaintenanceCompleted` to the
topic `maintenance.completed` after the database change is saved. resident-service consumes it
and stores a notification for the resident. Details and a sequence diagram are in
[docs/architecture.md](docs/architecture.md).

See the messages on the topic:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic maintenance.completed --from-beginning
```

## Running the tests

Backend (Java 21). The tests that load a Spring context use real Postgres databases, so start
them first, then run all modules from the repository root:

```bash
docker compose up -d resident-db billing-db maintenance-db reservation-db
./mvnw clean test
```

Frontend:

```bash
cd frontend && npm ci && npm run lint && npm run build
```

GitHub Actions runs the same on every push and pull request (`.github/workflows/ci.yml`), and
the `main` branch requires green checks before a pull request can be merged.

## Developing in the IDE

Run only the infrastructure in Docker and the services from the IDE:

```bash
docker compose up -d resident-db billing-db maintenance-db reservation-db kafka consul consul-config
```

- Install the shared library once (and after changing it): `mvn install -pl security-common`.
  Not needed for `docker compose` or when running `./mvnw test` from the root.
- Each module reads its settings from the environment. Copy the module's `.env.example` to
  `.env` (gitignored) and fill it in: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and
  `BOARD_SEED_PASSWORD` / `WORKER_SEED_PASSWORD` / `RESIDENT_SERVICE_URL` where listed.
  `JWT_SECRET` must be identical everywhere.
- Services started from the IDE register in Consul with their IP, so the gateway (also run from
  the IDE, or in Docker) can route to them. Consul must be running for the gateway to find them.
- Frontend: `cd frontend && npm run dev` (it uses `http://localhost:8080` for the gateway unless
  `frontend/.env` says otherwise).

## More documentation

- [Architecture: the MaintenanceCompleted event flow](docs/architecture.md)
- [Project overview and user stories](docs/requirements.md)
- [Exam checklist and status](docs/exam-checklist.md)
