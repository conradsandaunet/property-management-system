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
