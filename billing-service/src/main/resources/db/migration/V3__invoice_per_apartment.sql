ALTER TABLE invoice ADD COLUMN apartment_id BIGINT;
ALTER TABLE invoice ADD COLUMN apartment_number VARCHAR(10);

-- Eneste fakturaer som finnes er seed for residentId=1, som bor i apartment 1 ("SEED-1")
UPDATE invoice SET apartment_id = 1, apartment_number = 'SEED-1';

ALTER TABLE invoice ALTER COLUMN apartment_id SET NOT NULL;
ALTER TABLE invoice ALTER COLUMN apartment_number SET NOT NULL;

ALTER TABLE invoice DROP CONSTRAINT uq_invoice_resident_period;
DROP INDEX idx_invoice_resident_id;
ALTER TABLE invoice DROP COLUMN resident_id;

ALTER TABLE invoice ADD CONSTRAINT uq_invoice_apartment_period UNIQUE (apartment_id, period);
CREATE INDEX idx_invoice_period ON invoice (period);

ALTER TABLE invoice ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE invoice ADD COLUMN paid_by VARCHAR(50);ii