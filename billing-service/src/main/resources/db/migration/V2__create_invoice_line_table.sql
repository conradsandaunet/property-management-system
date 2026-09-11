CREATE SEQUENCE invoice_line_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE invoice_line (
    id BIGINT NOT NULL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id)
);