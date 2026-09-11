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

## Running the project

Checklist, in order:

1. **Databases** — start all four Postgres containers:
   ```bash
   docker compose up -d
   ```
2. **resident-service** — copy `resident-service/.env.example` to
   `resident-service/.env` and fill in real values (see below), then:
   ```bash
   mvn -pl resident-service spring-boot:run
   ```
   Runs on `http://localhost:8081`.
3. **billing-service** *(skeleton only, no endpoints yet)*:
   ```bash
   mvn -pl billing-service spring-boot:run
   ```
   Runs on `http://localhost:8082`.
4. **maintenance-service** *(skeleton only, no endpoints yet)*:
   ```bash
   mvn -pl maintenance-service spring-boot:run
   ```
   Runs on `http://localhost:8083`.
5. **reservation-service** *(skeleton only, no endpoints yet)*:
   ```bash
   mvn -pl reservation-service spring-boot:run
   ```
   Runs on `http://localhost:8084`.
6. **frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Runs on `http://localhost:5173`. Copy `frontend/.env.example` to
   `frontend/.env` if it's missing (already points at `resident-service` by
   default).

### resident-service environment variables

`resident-service` requires two settings before it will start — there is no
default, so the app fails fast on startup if either is missing:

- `JWT_SECRET` — signing key for issued JWTs
- `BOARD_SEED_PASSWORD` — password for the automatically seeded initial
  board account (`board@example.com`), created on first startup if the
  `resident` table is empty

Copy `resident-service/.env.example` to `resident-service/.env` and fill in
real values — the `.env` file is loaded automatically at startup (no extra
setup needed), whether you run via `mvn` or your IDE's run button. `.env` is
gitignored, so each developer (and the grader) sets their own.
