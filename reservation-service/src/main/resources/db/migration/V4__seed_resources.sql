-- Shared resources residents can book. Without these the booking page has nothing to show.
INSERT INTO resource (name, type, status) VALUES
    ('Guest parking P1', 'PARKING', 'AVAILABLE'),
    ('Guest parking P2', 'PARKING', 'AVAILABLE'),
    ('Common room', 'COMMON_ROOM', 'AVAILABLE');
