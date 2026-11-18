-- Buildings become their own table instead of a name repeated on every apartment.
CREATE SEQUENCE building_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE building (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Building names are unique regardless of case, like the old per-apartment check treated them.
CREATE UNIQUE INDEX uq_building_name ON building (lower(name));

-- One building per distinct name already used by an apartment (first spelling wins).
INSERT INTO building (id, name)
SELECT nextval('building_seq'), name
FROM (SELECT DISTINCT ON (lower(building_name)) building_name AS name
      FROM apartment
      ORDER BY lower(building_name), building_name) existing;

ALTER TABLE apartment ADD COLUMN building_id BIGINT;

UPDATE apartment a SET building_id = b.id FROM building b WHERE lower(b.name) = lower(a.building_name);

ALTER TABLE apartment ALTER COLUMN building_id SET NOT NULL;
ALTER TABLE apartment ADD CONSTRAINT fk_apartment_building FOREIGN KEY (building_id) REFERENCES building (id);
ALTER TABLE apartment DROP COLUMN building_name;

CREATE INDEX idx_apartment_building ON apartment (building_id);
