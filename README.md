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
