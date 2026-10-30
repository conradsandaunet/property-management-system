ALTER TABLE maintenance_worker ADD COLUMN password_hash VARCHAR(255);

CREATE UNIQUE INDEX uq_maintenance_worker_email ON maintenance_worker (email);
