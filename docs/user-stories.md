# User stories

Scenarios an examiner can run to see the functionality. Every user story in
[requirements.md](requirements.md) is listed below with how to try it. 31 of the 32 stories are
implemented, and the one that is not is marked. The system is started as described in the
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

### Resident service

| Story | Sign in as | How to try it |
|---|---|---|
| Log in and see my apartment (number, floor, building) | `kari@example.com` | Home |
| See who is registered in my household | `kari@example.com` | Account, Household |
| Update my contact information | `kari@example.com` | Account, edit email and phone |
| See the status of my apartment (Active resident) | `kari@example.com` | Account |
| Overview of the building (apartments, occupied, vacant, residents) | `board@example.com` | Overview |
| Add, edit and remove residents | `board@example.com` | Property, Residents tab |
| Add, edit and remove apartments | `board@example.com` | Property, Buildings tab, open a building |
| Search for a resident by name or apartment | `board@example.com` | Property, Residents tab, search field |
| Upload and remove photos of an apartment | `board@example.com` | Property, open an apartment, Photos |
| Add, edit and remove buildings | `board@example.com` | Property, Buildings tab |

### Billing service

| Story | Sign in as | How to try it |
|---|---|---|
| See my monthly invoice | `kari@example.com` | Invoices |
| Simulate paying my invoice | `kari@example.com` | Invoices, pay the current invoice |
| See a history of previous invoices | `kari@example.com` | Invoices, the list below the current invoice |
| Generate monthly invoices for all apartments | `board@example.com` | Billing, Generate invoices |
| Payment overview (paid, pending, overdue) for the building | `board@example.com` | Billing |
| Mark an invoice as paid manually | `board@example.com` | Billing, Mark as paid on an invoice |

### Maintenance service

| Story | Sign in as | How to try it |
|---|---|---|
| Create a maintenance request | `kari@example.com` | Maintenance, Report an issue |
| See the status of my requests | `kari@example.com` | Maintenance |
| Comment on an open request | `kari@example.com` | Maintenance, Comments |
| See all open requests by priority | `board@example.com` | Maintenance |
| Assign a request to a worker | `board@example.com` | Maintenance, Assign |
| Override the priority or status of a request | **Not implemented** | |
| See my assigned work orders | `per@example.com` at `/worker/login` | Work orders |
| Accept, start and complete a request | `per@example.com` at `/worker/login` | Work orders, Start work, then Mark as done |
| `MaintenanceCompleted` event published, resident notified | `kari@example.com` | Notifications, after the worker completes a request |
| Comment on a request as a worker | `per@example.com` at `/worker/login` | Work orders, Comments |
| See completed work orders | `per@example.com` at `/worker/login` | Work orders, Completed |

### Reservation service

| Story | Sign in as | How to try it |
|---|---|---|
| See available time slots for a resource | `kari@example.com` | Bookings |
| Reserve a resource for a time slot | `kari@example.com` | Bookings |
| Cancel my own reservation | `kari@example.com` | Bookings |
| See all reservations for a resource | `board@example.com` | Resources |
| Block a resource for a period | `board@example.com` | Resources, Block |

## End-to-end demo

As `kari@example.com` report a maintenance issue. As `board@example.com` assign it to Per. As
`per@example.com` at `/worker/login`, accept, start and complete it. Sign in as Kari again and
open Notifications. The notification is created from an asynchronous event, see
[the event flow](architecture.md#event-flow-maintenancecompleted).
