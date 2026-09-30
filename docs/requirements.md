# 2. User Stories

Priority: 🔴 Must-have (MVP) · 🟡 Should-have (if time) · ⚪ Nice-to-have (out of scope)

## 2.1 Resident Service

### Resident
- [x] 🔴 As a resident, I want to log in and see my apartment (number, floor, building name), so that I can confirm my housing information is correct.
- [ ] 🔴 As a resident, I want to see who is registered in my household, so that I know who is listed on the apartment.
- [ ] 🔴 As a resident, I want to update my contact information (email, phone), so that the board and maintenance staff can reach me.
- [ ] 🟡 As a resident, I want to see the status of my apartment (e.g. "Active resident"), so that I know my registration is valid

### Board / Property Manager
- [ ] 🔴 As a board member, I want to see an overview of the building (number of apartments, occupied, vacant, number of residents), so that I have a full overview of the property.
- [ ] 🔴 As a board member, I want to add, edit, and remove residents from an apartment, so that the registry is always up to date when people move in/out.
- [ ] 🟡 As a board member, I want to search for a resident by name or apartment number, so that I can quickly find the right information.

## 2.2 Billing Service

### Resident
- [x] 🔴 As a resident, I want to see my monthly invoice (shared costs, water, parking, etc.), so that I know what I owe.
- [x] 🔴 As a resident, I want to simulate paying my invoice, so that the status changes to "Paid".
- [x] 🟡 As a resident, I want to see a history of previous invoices, so that I can check whether I've paid for past months.

### Board / Property Manager
- [ ] 🔴 As a property manager, I want to generate monthly invoices for all apartments, so that I don't have to create them manually one by one.
- [ ] 🔴 As a property manager, I want to see an overview of payment status (paid / pending / overdue) for the whole building, so that I can follow up on outstanding payments.
- [ ] 🟡 As a property manager, I want to manually mark an invoice as paid, so that I can handle payments made outside the system (e.g. a bank transfer registered externally).

## 2.3 Maintenance Service

### Resident
- [ ] 🔴 As a resident, I want to create a maintenance request with category, location, description, and priority, so that I can report an issue in my apartment.
- [ ] 🔴 As a resident, I want to see the status of my own requests (Submitted → Assigned → In progress → Resolved), so that I know where my case stands.
- [ ] 🟡 As a resident, I want to add a comment to an open request, so that I can provide more information as it comes up.

### Board / Property Manager
- [ ] 🔴 As a board member, I want to see all open maintenance requests in the building with priority (🔴🟡🟢), so that I can quickly see what's most urgent.
- [ ] 🔴 As a board member, I want to assign a request to a maintenance worker, so that responsibility is clearly placed.
- [ ] 🟡 As a board member, I want to manually change the priority or status of a request, so that I can override it when needed.

### Maintenance Staff
- [ ] 🔴 As a maintenance worker, I want to see a list of my assigned work orders, so that I know what I need to do today.
- [ ] 🔴 As a maintenance worker, I want to change the status of a request (Accept → Start work → Complete), so that progress is visible to others.
- [ ] 🔴 As a maintenance worker, I want the system to publish a `MaintenanceCompleted` event when I complete a request, so that other services can react to it (e.g. notify the resident).
- [ ] 🟡 As a maintenance worker, I want to add a comment to a request, so that I can document what was done.

## 2.4 Reservation Service

### Resident
- [ ] 🔴 As a resident, I want to see available time slots for a shared resource (e.g. guest parking, common room), so that I know when I can book it.
- [ ] 🔴 As a resident, I want to reserve a shared resource for a given time slot, so that I secure access to it.
- [ ] 🟡 As a resident, I want to cancel my own reservation, so that the resource becomes available to others again.

### Board / Property Manager
- [ ] 🔴 As a board member, I want to see all reservations for a given resource, so that I have an overview of its usage.
- [ ] 🟡 As a board member, I want to block a resource (e.g. set the common room to "MAINTENANCE" for a period), so that residents cannot book it while it's unavailable.
