# property-management-system
A microservices-based property management system handling residents, billing, maintance requests and reservations, built as part of the PG3402 Microservices exam project.

## Documentation
- [Project Overview & User Stories](docs/requirements.md) - background, architecture, user roles, and MVP scope

## Architecture

| Service | Responsibility |
|---|---|
| Resident Service | Buildings, apartments, and resident data |
| Billing Service | Monthly invoices and payment status |
| Maintenance Service | Maintenance requests and work orders |
| Reservation Service | Booking of shared resources |

## Gateway

The browser (and the Vite dev server) only talks to the **gateway** on port
8080. It routes `/api/**` to the services (stripping the `/api` prefix),
handles CORS, and rejects requests without a valid JWT with `401`. Services
still validate the token themselves and enforce roles.

```
browser -> gateway :8080 -> resident :8081 | billing :8082 | maintenance :8083 | reservation :8084
```

| Path | Service |
|---|---|
| `/api/auth`, `/api/residents`, `/api/apartments` | resident-service |
| `/api/invoices` | billing-service |
| `/api/maintenance`, `/api/workers` | maintenance-service |
| `/api/reservations`, `/api/resources` | reservation-service |

Setup: copy `gateway/.env.example` to `gateway/.env` (all values are required;
there are no defaults). `JWT_SECRET` must be identical in every service and the
gateway. Run it with `mvn -pl gateway spring-boot:run` after the services are
up, then `npm run dev` in `frontend/` (optionally copy `frontend/.env.example`
to `frontend/.env`; it defaults to `http://localhost:8080`). The services are
still reachable directly on 8081-8084 until Docker is in place.

## Running with Docker

Everything (databases, four services, gateway and frontend) starts with one command:

```bash
cp .env.example .env     # then set JWT_SECRET and BOARD_SEED_PASSWORD
docker compose up --build
```

Open http://localhost:3000 (change with `FRONTEND_PORT` in `.env`).

- Only the frontend (nginx) is published. It serves the React app and proxies `/api`
  to the gateway; the gateway and the services are reachable inside the compose
  network only, so nothing can bypass the JWT check.
- The databases still publish 5432-5435 so you can use IntelliJ's database panel
  or run one service from the IDE against the containerised databases.
- Service containers start in dependency order using `/actuator/health`
  (service waits for its database; billing also waits for resident; the gateway
  waits for all four; the frontend waits for the gateway).
- Database credentials are the local dev ones in `docker-compose.yml`; secrets
  (`JWT_SECRET`, `BOARD_SEED_PASSWORD`) come from the root `.env`, which is gitignored.

Only the databases (for developing in the IDE):

```bash
docker compose up -d resident-db billing-db maintenance-db reservation-db
```

Every service reads its database settings from the environment: `DB_URL`,
`DB_USER`, `DB_PASSWORD` (billing also needs `RESIDENT_SERVICE_URL`). For IDE runs
these are in each module's `.env` (see its `.env.example`).

## First-time setup

`resident-service`, `billing-service`, and `reservation-service` all share
JWT issuing/validation code from a local library module, `security-common`
(not a deployable service — no port, nothing to run). Before building or
running any of those three services for the first time (or after pulling
changes to `security-common`), install it into your local Maven repo:

```bash
mvn install -pl security-common
```

Without this, the other services will fail to resolve the `security-common`
dependency.

## Running resident-service

`resident-service` requires two settings before it will start — there is no
default, so the app fails fast on startup if either is missing:

- `JWT_SECRET` — signing key for issued JWTs
- `BOARD_SEED_PASSWORD` — password for the automatically seeded initial
  board account (`board@example.com`), created on first startup if the
  `resident` table is empty

Copy `resident-service/.env.example` to `resident-service/.env` and fill in
real values, then run the service as normal (IDE run button, or
`mvn -pl resident-service spring-boot:run`) — the `.env` file is loaded
automatically at startup, no extra setup needed. `.env` is gitignored, so
each developer (and the grader) sets their own.

## Running billing-service

`billing-service` never authenticates anyone itself — login lives in
resident-service. It only validates the JWT resident-service issued, so it
needs one setting before it will start — there is no default, so the app
fails fast on startup if it's missing:

* `JWT_SECRET` — must be the **exact same value** as resident-service's
  `JWT_SECRET`, since billing-service verifies the signature on tokens
  resident-service signed

Copy `billing-service/.env.example` to `billing-service/.env` and set
`JWT_SECRET` to the same value you used in `resident-service/.env`, then
run the service as normal (IDE run button, or
`mvn -pl billing-service spring-boot:run`) — the `.env` file is loaded
automatically at startup, no extra setup needed. `.env` is gitignored, so
each developer (and the grader) sets their own.

A demo invoice is seeded on first startup for residentId=1, which is
always resident-service's seeded board account (`board@example.com`) on a
fresh database — so `GET /invoices/current` has something to return right
after both services start, without creating any real residents first.

### Trying it out

```bash
# 1. start both databases
docker compose up -d

# 2. run resident-service (port 8081) and billing-service (port 8082)
mvn -pl resident-service spring-boot:run
mvn -pl billing-service spring-boot:run

# 3. log in via resident-service to get a token
curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"board@example.com","password":"<your BOARD_SEED_PASSWORD>"}'

# 4. use that token against billing-service
TOKEN="<paste the token value here>"
curl -s http://localhost:8082/invoices/current -H "Authorization: Bearer $TOKEN"
```
test