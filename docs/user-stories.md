# User stories

Scenarios an examiner can run to see the functionality. The complete list of user stories, with
priorities, is in [requirements.md](requirements.md). The system is started as described in the
[README](../README.md#2-build-start-and-run).

## Contents

- [Demo users and data](#demo-users-and-data)
- [Scenarios to try](#scenarios-to-try)
- [End-to-end demo](#end-to-end-demo)

## Demo users and data

On first start every service fills its empty database with demo data: two buildings
("Solsiden Borettslag" with 6 apartments and "Havneparken" with 2 vacant ones), 8 residents,
invoices for last and this month (one overdue), 7 maintenance requests in all statuses, 3
workers, 5 resources (3 parking spaces, a common room and a roof terrace), some upcoming
bookings, and photos of apartments and a facade. Set `SEED_DEMO_DATA=false` to start with only
the board account and the workers.

| Role | Sign in at | Email | Password |
|---|---|---|---|
| Board member | http://localhost:3000 | `board@example.com` | `BOARD_SEED_PASSWORD` (`change-this-password` by default) |
| Resident | http://localhost:3000 | `kari@example.com` (also `ola@`, `ingrid@`, `lars@`, `sofie@`, `emil@`, `nora@example.com`) | `demo-resident-pw` |
| Maintenance worker | http://localhost:3000/worker/login | `per@example.com` (plumbing, heating), `jon@example.com` (electrical, appliances), `ola@example.com` (all) | `WORKER_SEED_PASSWORD` (`change-this-password` by default) |

Seeding only happens when a database is empty, so it does not run again on an existing
volume. Use `docker compose down -v` to reset. When you run the frontend with `npm run dev`,
use http://localhost:5173 instead of port 3000.

## Scenarios to try

| Story | Sign in as | How to try it |
|---|---|---|
| Resident sees apartment, household and contact details | `kari@example.com` | Home, then Account |
| Resident reports a maintenance issue and follows its status | `kari@example.com` | Maintenance, then report a new issue |
| Board assigns a request to a worker | `board@example.com` | Maintenance, then pick a worker |
| Worker accepts, starts and completes a request | `per@example.com` at `/worker/login` | Work orders |
| Resident gets a notification when the request is completed (asynchronous event) | `kari@example.com` | Notifications, after the worker completes a request |
| Resident and worker comment on a request | `kari@example.com` and `per@example.com` | Open a request and add a comment |
| Board manages residents and apartments, and apartment photos | `board@example.com` | Residents and Apartments |
| Board generates monthly invoices and marks them paid | `board@example.com` | Invoices |
| Resident sees invoices and simulates payment | `kari@example.com` | Invoices |
| Resident books and cancels a shared resource | `kari@example.com` | Reservations |
| Board blocks a resource for a period | `board@example.com` | Resources |

## End-to-end demo

As `kari@example.com` report a maintenance issue. As `board@example.com` assign it to Per. As
`per@example.com` at `/worker/login`, accept, start and complete it. Sign in as Kari again and
open Notifications. The notification is created from an asynchronous event, see
[the event flow](architecture.md#event-flow-maintenancecompleted).
