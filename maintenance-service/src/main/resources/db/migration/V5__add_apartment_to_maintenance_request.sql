ALTER TABLE maintenance_request
    ADD COLUMN apartment_id BIGINT,
    ADD COLUMN apartment_number VARCHAR(20),
    ADD COLUMN building_name VARCHAR(255);
