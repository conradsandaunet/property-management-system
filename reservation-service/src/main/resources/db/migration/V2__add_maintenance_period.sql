-- Lets the board block a resource for a limited period (e.g. common room under maintenance 20-25 Oct).
-- Both NULL = no scheduled block.
ALTER TABLE resource ADD COLUMN maintenance_from TIMESTAMP;
ALTER TABLE resource ADD COLUMN maintenance_to TIMESTAMP;