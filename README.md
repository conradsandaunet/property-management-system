# Property Management System

A microservices-based system for a housing cooperative. Built for the PG3402 Microservices exam.

## Contents

1. [Overview](#1-overview)
2. [Build, start and run](#2-build-start-and-run)
3. [User stories](#3-user-stories)
4. [Architecture](#4-architecture)
5. [Team and contributions](#5-team-and-contributions)

## 1. Overview

Residents report maintenance issues, see and pay invoices and book shared resources. Board
members manage the building, assign work and generate invoices, and maintenance workers handle
their assigned work orders.

The system has four business services (resident, billing, maintenance and reservation), a
gateway that is the single entry point, and a React frontend. Each service has its own
Postgres database. The services talk to each other synchronously over REST and
asynchronously through Kafka, and register in Consul for service discovery. Everything runs
with `docker compose up`.

## 2. Build, start and run

**You need:** Docker with Compose. Nothing else has to be installed.

```bash
cp .env.example .env
docker compose up --build
```

The first build takes a few minutes. When `docker compose ps` shows every container as
`healthy`, open **http://localhost:3000**. The default values in `.env.example` work as they
are, so nothing has to be edited to try the project.

| Address | What |
|---|---|
| http://localhost:3000 | The application |
| http://localhost:8500 | Consul UI (services and their health) |

To start from scratch with empty databases and fresh demo data: `docker compose down -v`.

There are two ways to run the project:

| Way | Use it for | Instructions |
|---|---|---|
| Docker Compose (recommended) | Running the whole system | This section, and [docs/running.md](docs/running.md#docker-compose) |
| Services in the IDE, infrastructure in Docker | Developing or debugging one service | [docs/running.md](docs/running.md#services-in-the-ide) |

The images are built locally from this repository. They are not published to Docker Hub.

### Running the tests

The backend tests use real Postgres databases, so start them first (Java 21 and Docker are needed):

```bash
docker compose up -d --wait resident-db billing-db maintenance-db reservation-db
./mvnw clean test
```

We chose real Postgres over an in-memory database because several guarantees live in the
database: the exclusion constraint that stops double bookings, the advisory lock that lets several
instances seed safely, and the Flyway migrations. Starting the databases by hand is a compromise.
Testcontainers would be the better solution, since `./mvnw test` would then start its own
databases. We did not add it because it would change the test setup of every module, and that was
too risky this late in the project. It is the first thing we would do next.

More: [running the tests](docs/running.md#running-the-tests) and
[troubleshooting](docs/running.md#troubleshooting).

## 3. User stories

On first start every service fills its database with demo data (two buildings, residents,
invoices, maintenance requests, workers, resources and bookings). Sign in with one of these:

| Role | Sign in at | Email | Password |
|---|---|---|---|
| Board member | http://localhost:3000 | `board@example.com` | `change-this-password` |
| Resident | http://localhost:3000 | `kari@example.com` | `demo-resident-pw` |
| Maintenance worker | http://localhost:3000/worker/login | `per@example.com` | `change-this-password` |

The board and worker passwords are `BOARD_SEED_PASSWORD` and `WORKER_SEED_PASSWORD` in `.env`.

**Suggested end-to-end demo:** as `kari@example.com` report a maintenance issue. As
`board@example.com` assign it to Per. As `per@example.com` at `/worker/login`, accept, start and
complete it. Sign in as Kari again and open Notifications.

Every user story, with how to try it, is in [docs/user-stories.md](docs/user-stories.md). 31 of
the 32 stories are implemented, and the one that is not is marked there. The complete list with
priorities is in [docs/requirements.md](docs/requirements.md).

## 4. Architecture

![Architecture diagram: the frontend and gateway route requests to four services, each with its own database. Billing and maintenance call resident over REST, and maintenance publishes events to Kafka, which resident consumes.](docs/images/diagram.png)

Solid blue arrows are synchronous calls (REST and database access). Dashed yellow arrows are
asynchronous events sent through Kafka. Every service and the gateway register in Consul.

| Kind | From | To | Why |
|---|---|---|---|
| Synchronous (REST) | gateway | the four services | Routing by path |
| Synchronous (REST) | billing-service | resident-service | Billing needs apartments and the signed-in resident to create and show invoices |
| Synchronous (REST) | maintenance-service | resident-service | When a resident reports an issue, maintenance looks up their apartment and stores it on the request, so workers see where to go |
| Asynchronous (Kafka) | maintenance-service | resident-service | A completed request notifies the resident, and the services do not depend on each other |

Each service owns its database and never reads another service's. reservation-service makes no
calls to other services.

Details are in [docs/architecture.md](docs/architecture.md): the services and their ports,
gateway routes and security, Consul, what happens when a service is slow or down, load balancing
and the Kafka event flow.

## 5. Team and contributions

| Member | Main responsibilities                                                                                                                                                                                                            |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Conrad Sandaunet | maintenance-service (requests, workers and worker login, work orders, comments), the `MaintenanceCompleted` event in Kafka, notifications in resident-service, and the frontend for maintenance, work orders and notifications   |
| Markus Nilsen | resident-service, Docker Compose setup, CI, healthchecks and login rate limiting, Consul (registration, central configuration, load balancing), demo data, multiple buildings and apartment photos, and the frontend restructure |
| Marija Dobrijevic | reservation-service (resources, bookings, blocking, double-booking protection), billing-service (invoices, payment overview, PDF), the `apartmentId` claim in the JWT, and the frontend for billing and reservations             |

Decisions about architecture, communication between services and which user stories to include
were made together. Work was done on feature branches and merged through pull requests with
passing CI.

---

More documentation: [Running the project](docs/running.md),
[User stories](docs/user-stories.md), [Architecture](docs/architecture.md),
[Requirements](docs/requirements.md)
