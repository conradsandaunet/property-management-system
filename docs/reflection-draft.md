# Individual reflection: property management system (PG3402)

> DRAFT. Rewrite in your own words and fix anything that doesn't match your experience. Text in
> [brackets] is for you to fill in. Keep it to two pages. Don't commit this file as it is, since each
> student hands in their own.

## The project and my part

We built a housing cooperative portal with four services (resident, billing, maintenance and
reservation), a gateway and a React frontend. Residents report problems, pay invoices and book
shared resources; the board manages buildings, residents, maintenance and billing; workers handle
work orders. My main parts were [the gateway, Docker Compose and CI, service discovery, load
balancing and central configuration in Consul, apartments, buildings and photos in the resident
service, the demo data, resilience, and the frontend redesign]. [My teammates did X and Y, for
example the Kafka event flow in maintenance.]

## Why microservices, and where I doubted it

The domain splits into parts that change and fail independently: a booking mistake shouldn't stop
invoices, and the maintenance flow gives a natural asynchronous event. That is the argument for
microservices here. I also saw the cost: more moving parts to start and debug, data spread over
four databases, and consistency problems that a single database would have solved for free. Two
examples are the **double-booking race** when reservation runs as several instances (solved with an
exclusion constraint in the database, not with code), and **ids that point across services**
(billing stores an apartment id, so the board pages ask resident-service for names).

I deliberately kept residents, apartments and buildings in **one** service. They share foreign
keys and transactions (an apartment can't be removed while people live in it), and splitting them
would have turned every list into several network calls for no gain in scaling. A good microservice
boundary follows the data and the transactions, not the number of entities.

## Choices and what they cost

- **Synchronous or asynchronous.** Billing calls resident-service over REST because it needs the
  answer now (the apartments to invoice). Completing a work order publishes an event on Kafka,
  because the notification can wait and maintenance shouldn't know about residents. The price is that
  the event can be **lost** if Kafka is down at that moment. A transactional outbox would fix it; we
  documented it instead of building it.
- **One entry point.** The gateway validates the JWT and the services validate it again, so a
  service is safe even if someone reaches it directly. The login rate limit lives in memory, so it
  only works with one gateway instance. I chose the simple solution and wrote down the limit.
- **Consul for discovery, health, load balancing and configuration.** One tool covered four
  requirements. Shared settings (timeouts, limits, Kafka topic, CORS) sit in its key/value store,
  while secrets and per-environment addresses stay in `.env`, because Consul in dev mode is open and
  unencrypted. A change needs a restart, which is fine for settings that rarely change.
- **Resilience.** At first nothing stopped a hanging service from tying up its callers. I added
  timeouts, retries (reads only, so a write is never repeated), and circuit breakers, and showed it
  by stopping billing while the rest kept working. Testing taught me something I wouldn't have
  guessed: with the breaker as the outer filter the retry never fired, and swapping the order fixed it.
- **Docker Compose, not Kubernetes.** I can scale a service by hand (`--scale`) and the gateway
  spreads the load, but nothing scales on load. That needs metrics and an autoscaler, for example a
  Horizontal Pod Autoscaler. I only prepared reservation for several instances, since it has no
  internal state; the other services have seeding and in-memory state that isn't safe to duplicate.
- **Payment is simulated.** An invoice isn't a card checkout. Norwegian invoices are paid in the
  online bank with an account number and KID, so we show those (the KID with a MOD10 check digit)
  and simulate the bank confirming. Stripe would have needed keys and a public webhook, which would
  make the project harder for an examiner to run.

## What we left out, and why

From the user stories we did not build: the board overriding a request's priority or status
(should-have), workers seeing completed work orders (nice-to-have), password change, and a page for
creating workers (the API exists). We spent the time on the must-haves and on the infrastructure
requirements for the higher grades, because a service that works and can be started with one command
matters more than the last nice-to-have. [Add how you and the team decided this.] We added things
the stories didn't ask for: apartment and building photos, multiple buildings, comments on requests,
and a board and a resident view for board members.

## Lessons learned

1. **Check the running system, not only the tests.** Unit tests were green while the application
   crashed at start-up (a lazy-loaded entity outside a transaction) and a gateway route was missing.
   I found both only by starting everything and clicking through it.
2. **Stale Docker images cost me a lot of time.** I repeatedly looked for bugs in code that wasn't the
   code running. `docker compose up --build` and a hard refresh are the first things I try now.
3. **Decide configuration early.** Moving settings to Consul late meant touching many files and
   tests. [Add what you would do differently.]
4. **Working as a team.** Required CI checks and pull requests caught compile errors that my IDE
   hid. [Add your own experience: reviews, merge conflicts, splitting the work.]
5. **Be honest about limits.** Every shortcut (lost events, per-instance rate limit, manual scaling,
   tokens that live for an hour after a user is removed) is written down in the architecture
   document instead of hidden.

[Optional: one paragraph on how long things took compared with your estimate, and what you would
change if you started over, for example testing against real databases with Testcontainers.]
