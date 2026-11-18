-- One facade photo per building, in its own table so listing buildings doesn't load the image.
CREATE TABLE building_facade (
    building_id BIGINT NOT NULL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes INTEGER NOT NULL,
    data BYTEA NOT NULL,
    CONSTRAINT fk_building_facade_building FOREIGN KEY (building_id) REFERENCES building (id) ON DELETE CASCADE
);
