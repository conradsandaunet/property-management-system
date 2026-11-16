-- The overlap check in ReservationService is check-then-insert, so two requests (or two service
-- instances) can both pass it. Let the database refuse overlapping ACTIVE reservations per
-- resource. Back-to-back bookings (one ends 10:00, next starts 10:00) are still allowed because
-- tsrange is half-open.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservation
    ADD CONSTRAINT no_overlapping_active_reservations
    EXCLUDE USING gist (resource_id WITH =, tsrange(start_time, end_time) WITH &&)
    WHERE (status = 'ACTIVE');
