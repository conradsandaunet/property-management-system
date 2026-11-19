# Running the project

How to build, start and run the system. The short version is in the [README](../README.md#2-build-start-and-run).
This page has the details and the second way to run it.

## Contents

- [Docker Compose](#docker-compose)
- [Services in the IDE](#services-in-the-ide)
- [Running the tests](#running-the-tests)
- [Troubleshooting](#troubleshooting)

## Docker Compose

This is the recommended way. It starts the databases, Kafka, Consul, the four services, the
gateway and the frontend.

**You need:** Docker with Compose. Nothing else has to be installed.

1. Unzip the project and open a terminal in the project folder.
2. Create the settings file:

```bash
cp .env.example .env
```

3. Start everything (the first build takes a few minutes):

```bash
docker compose up --build
```

4. Open **http://localhost:3000** and sign in with one of the [demo users](user-stories.md#demo-users-and-data).

The default values in `.env.example` work as they are, so nothing has to be edited to try the
project. With the defaults, the board and the workers use the password `change-this-password`.
Change the values in `.env` for anything other than a demo. `JWT_SECRET` must be at least 32
characters and is shared by all services.

| Address | What |
|---|---|
| http://localhost:3000 | The application |
| http://localhost:8500 | Consul UI (services and their health, configuration) |

Only the frontend (nginx) is published for the application. It serves the React app and
proxies `/api` to the gateway. The gateway and the services are reachable inside the Docker
network only, so no request can skip the JWT check. The databases (5432-5435) and Consul
(8500) are also published for inspection.

To stop: `Ctrl+C`, or `docker compose down`. To start from scratch with empty databases and
fresh demo data: `docker compose down -v`. Set `FRONTEND_PORT` in `.env` if port 3000 is busy.

## Services in the IDE

Run the infrastructure in Docker and the services from your IDE or the terminal. Java 21 and
Node 22 are required.

1. Start the databases, Kafka and Consul:

```bash
docker compose up -d resident-db billing-db maintenance-db reservation-db kafka consul consul-config
```

2. Install the shared library once, and again after changing it:

```bash
./mvnw -pl security-common install -DskipTests
```

3. In each module (`resident-service`, `billing-service`, `maintenance-service`,
   `reservation-service` and `gateway`), copy `.env.example` to `.env`. The services read it
   when they start. The values are already filled in for local use, and `JWT_SECRET` must be
   the same in every module. The modules use these ports and databases:

| Module | Port | Database (`DB_URL`) |
|---|---|---|
| resident-service | 8081 | `jdbc:postgresql://localhost:5432/resident` |
| billing-service | 8082 | `jdbc:postgresql://localhost:5433/billing` |
| maintenance-service | 8083 | `jdbc:postgresql://localhost:5434/maintenance` |
| reservation-service | 8084 | `jdbc:postgresql://localhost:5435/reservation` |
| gateway | 8080 | none |

4. Start each module, either by running its main class in the IDE or from the repository root,
   one terminal per module (start the gateway last):

```bash
./mvnw -pl resident-service spring-boot:run
```

Replace `resident-service` with `billing-service`, `maintenance-service`,
`reservation-service` or `gateway`.

5. Start the frontend and open **http://localhost:5173**:

```bash
cd frontend && npm ci && npm run dev
```

The dev server sends `/api` to the gateway on `http://localhost:8080`. Set `GATEWAY_URL` in
`frontend/.env` to use another address.

Services started this way register in Consul with their IP address, so the gateway can route
to them. Do not run the same service in Docker and in the IDE at the same time, since they use
the same port.

## Running the tests

The backend tests that load a Spring context use real Postgres databases, so start them
first. Java 21 and Docker are required. Then run all modules from the repository root:

```bash
docker compose up -d --wait resident-db billing-db maintenance-db reservation-db
./mvnw clean test
```

`--wait` returns when the databases are healthy. The tests expect them on ports 5432 to 5435
(the ports `docker-compose.yml` publishes, and the ones GitHub Actions uses).

The tests use the same databases as the application. If you run them before you have started the
system for the first time, they create the board account and the workers with the same passwords
as in `.env.example`, so you can still log in as described in the README. To start from scratch
with empty databases and fresh demo data, run `docker compose down -v` first.

**Why real Postgres and not an in-memory database?** Several guarantees live in the database,
and an in-memory database such as H2 would pass without proving them: the exclusion constraint that
stops double bookings (`DoubleBookingTest`), the advisory lock that lets several instances seed
safely (`SeedLockTest`), and the Flyway migrations themselves. Tests that only use mocks (most
service tests) do not need a database.

If a database is not running, the tests that load a Spring context fail with
`Connection to localhost:5432 refused` while the rest pass. Start the databases and run again.

Starting the databases from Docker by hand is a trade-off we accepted. Testcontainers would
be the better solution: it starts and stops the databases from the test run itself, so
`./mvnw test` would work on its own. We did not add it because it would change the test setup of
every module, and we judged that too risky this late in the project. Docker is needed
either way.

Frontend (Node 22):

```bash
cd frontend && npm ci && npm run lint && npm run build
```

GitHub Actions runs the same checks on every push and pull request, and `main` requires green
checks before a pull request can be merged.

## Troubleshooting

| Problem | Fix |
|---|---|
| `port is already allocated` (for example 5432) | Another container or Postgres uses the port. Stop it, or change the published port in `docker-compose.yml` |
| A service exits with `Could not resolve placeholder` | A value is missing in `.env`. Compare it with `.env.example` |
| In the IDE: `'url' must start with "jdbc"` | `DB_URL`, `DB_USER` or `DB_PASSWORD` is missing in the module's `.env`. Copy them from `.env.example` |
| In the IDE: the frontend shows `ECONNREFUSED` or `502` for `/api/...` | The gateway is not running on port 8080. Start it (step 4 above) |
| Port 3000 is busy | Set `FRONTEND_PORT` in `.env` |
| Tests fail with `Connection to localhost:5432 refused` | The test databases are not running. See [running the tests](#running-the-tests) |
| Login keeps saying wrong email or password | Logins are limited to 5 per minute. Wait a minute |
| No demo data after start | The databases already had data. Run `docker compose down -v` and start again |
| Docker build fails with `parent snapshot ... does not exist` | Corrupt build cache: `docker builder prune -f` and build again |
