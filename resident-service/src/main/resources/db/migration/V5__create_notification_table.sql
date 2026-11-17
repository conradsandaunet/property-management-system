CREATE SEQUENCE notification_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notification (
    id BIGINT NOT NULL PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_resident FOREIGN KEY (resident_id) REFERENCES resident_id (id),
    CONSTRAINT uq_notification_resident_request UNIQUE (resident_id, request_id)
);

CREATE INDEX idx_notification_resident ON notification (resident_id, created_at DESC);