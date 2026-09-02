CREATE SEQUENCE resident_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE resident (
    id BIGINT NOT NULL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    is_manager BOOLEAN NOT NULL DEFAULT FALSE,
    apartment_id BIGINT NOT NULL,
    CONSTRAINT fk_resident_apartment FOREIGN KEY (apartment_id) REFERENCES apartment (id)
);