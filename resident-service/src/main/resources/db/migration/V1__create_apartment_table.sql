CREATE SEQUENCE apartment_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE apartment (
    id BIGINT NOT NULL PRIMARY KEY,
    apartment_number VARCHAR(10) NOT NULL,
    floor INT NOT NULL,
    building_name VARCHAR(255) NOT NULL
);