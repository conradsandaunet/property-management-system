CREATE SEQUENCE invoice_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE invoice (
    id BIGINT NOT NULL PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    period varchar(7) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_invoice_resident_period UNIQUE (resident_id, period)
);

CREATE INDEX idx_invoice_resident_id ON invoice (resident_id);