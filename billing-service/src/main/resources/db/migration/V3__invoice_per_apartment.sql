-- Invoices belong to an apartment instead of a single resident
-- (user story: "generate monthly invoices for all apartments").

ALTER TABLE invoice ADD COLUMN apartment_id BIGINT;

-- The only existing invoice is the dev seed (residentId=1), who lives in apartment 1.
UPDATE invoice SET apartment_id = 1;

ALTER TABLE invoice ALTER COLUMN apartment_id SET NOT NULL;

ALTER TABLE invoice DROP CONSTRAINT uq_invoice_resident_period;
DROP INDEX idx_invoice_resident_id;
ALTER TABLE invoice DROP COLUMN resident_id;

ALTER TABLE invoice ADD CONSTRAINT uq_invoice_apartment_period UNIQUE (apartment_id, period);