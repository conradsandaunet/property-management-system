# Decisions, assumptions and simplifications

The choices we made while building the system, what we assumed about the domain, and where we
kept things simpler than a real system would. A short version is in the
[README](../README.md#6-decisions-assumptions-and-simplifications).

## Contents

- [Decisions](#decisions)
- [Assumptions](#assumptions)
- [Simplifications](#simplifications)
- [Left out](#left-out)

## Decisions

| Decision | Why | What it costs |
|---|---|---|
| Each service owns its database and never reads another service's | The services can change and run independently | No joins across services. Data a service needs from another is fetched through its API or kept as a copy (the notification table, the apartment on a maintenance request) |
| The gateway is the only entry point, and the services are not published | Nothing can skip the JWT check, and routing, CORS and the login rate limit live in one place | The gateway is a single point to keep running |
| The gateway validates the JWT, and each service validates it again | A service stays safe even if it is reached some other way, and it enforces its own roles | The check is done twice, and all services share one signing secret (`JWT_SECRET`) |
| Creating and checking tokens lives in one shared library, `security-common` | The gateway and every service handle tokens the same way, and the code is written once. Each service still decides which roles may call which endpoints | Every module depends on the library and has to be built with it, and a change to it affects all of them |
| Maintenance workers have their own table and login in maintenance-service, with a token subject like `worker:1` | Workers do not live in the building, so they are not residents. A worker token can never be read as a resident id | Identity is handled in two places, and the board has no single list of all users |
| A completed request notifies the resident through a Kafka event | Maintenance and resident do not depend on each other being up, and new consumers can be added without changing maintenance | The event can be lost if Kafka is down when it is published (no outbox), and the consumer must handle duplicates |
| The notification is stored in resident-service | Kafka can be emptied and the frontend cannot read from it. A notification also has state per resident (read or unread) | A table and an endpoint more in resident-service |
| billing-service calls resident-service over REST | Billing needs the apartments and the signed-in resident to create and show invoices | Billing depends on resident-service, so it uses timeouts, retries and a circuit breaker (see [architecture](architecture.md#when-a-service-is-slow-or-down)) |
| maintenance-service looks up the apartment in resident-service when a request is created, and stores it on the request | Workers need to know where to go, and a copy means work orders still load when resident-service is down | The apartment can be out of date if it changes later. If the lookup fails, the request is saved without an apartment |
| reservation-service makes no calls to other services | It identifies the user from the JWT and stores only the resident id, so it can run, scale and fail on its own | It does not know if a resident has been deactivated until the token expires |
| Double booking is stopped by a database constraint, not only by code | Two requests for the same slot at the same time give one success and one `409`, also with several instances | The rule lives in the database schema |
| One Consul for service discovery, health and shared configuration | One tool covers three needs, and the gateway needs no hard-coded addresses | Another container to run, and Consul runs in dev mode (see below). Secrets stay in `.env` |


## Assumptions

- Board members also live in the building, so they can switch between the board view and the
  resident view.
- Maintenance workers are external. They do not live in the building and are not residents.
- An apartment can have several residents.
- The system supports several buildings. Apartment numbers are only unique within a building.
- The board assigns work. A request has one assigned worker at a time.

## Simplifications

- **Payments are simulated.** There is no real payment provider. Paying an invoice only changes
  its status.
- **Demo credentials.** The passwords and secrets in `.env.example` are for demonstration only.
- **Kafka is one node without a volume.** The topic is emptied when the container is recreated,
  and there is no outbox or dead-letter topic, so an event can be lost. Notifications already
  stored in the database are not affected.
- **Consul runs in dev mode (in memory).** The shared configuration is loaded again at every
  start, so nothing is lost.
- **The login rate limit is kept in memory per gateway instance**, so only one gateway should
  run.
- **Only reservation-service is prepared and demonstrated with several instances.**
- **Notifications are fetched by polling every 30 seconds** instead of being pushed.
- **A token lives for 60 minutes and cannot be revoked**, so a deactivated resident keeps access
  until it expires.
- **Workers are created through the demo data or the API** (`POST /workers`). There is no screen
  for it.

## Left out

- **The board cannot override the priority or status of a request.** This is the one user story
  in [requirements.md](requirements.md) that is not implemented. We chose to spend the time on
  the stories needed for the main flow, and it is not clear that the board should overrule how
  a resident rates an issue, since the board already decides who does the work.
