CREATE SEQUENCE maintenance_request_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE maintenance_request (
    id BIGINT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maintenance_request_resident ON maintenance_request (resident_id);
CREATE INDEX idx_maintenance_request_status ON maintenance_request (status);
