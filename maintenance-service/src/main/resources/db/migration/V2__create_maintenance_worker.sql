CREATE SEQUENCE maintenance_worker_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE maintenance_worker (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE maintenance_worker_specialty (
    worker_id BIGINT NOT NULL REFERENCES maintenance_worker (id),
    category VARCHAR(50) NOT NULL,
    PRIMARY KEY (worker_id, category)
);

ALTER TABLE maintenance_request
    ADD COLUMN assigned_worker_id BIGINT REFERENCES maintenance_worker (id);

CREATE INDEX idx_maintenance_request_assigned_worker ON maintenance_request (assigned_worker_id);