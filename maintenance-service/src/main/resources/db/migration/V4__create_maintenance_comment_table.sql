CREATE SEQUENCE maintenance_comment_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE maintenance_comment (
    id BIGINT NOT NULL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES maintenance_request (id),
    author_type VARCHAR(20) NOT NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maintenance_comment_request ON maintenance_comment (request_id, created_at);