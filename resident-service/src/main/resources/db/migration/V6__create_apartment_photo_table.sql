-- Photos of an apartment, stored in the database so they follow the normal backup/volume of
-- resident-db and need no separate file storage. Deleting an apartment deletes its photos.
CREATE SEQUENCE apartment_photo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE apartment_photo (
    id BIGINT NOT NULL PRIMARY KEY,
    apartment_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes INTEGER NOT NULL,
    data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apartment_photo_apartment FOREIGN KEY (apartment_id) REFERENCES apartment (id) ON DELETE CASCADE
);

CREATE INDEX idx_apartment_photo_apartment ON apartment_photo (apartment_id, id);
