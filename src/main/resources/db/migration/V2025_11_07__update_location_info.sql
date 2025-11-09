ALTER TABLE marlin.Location
    DROP COLUMN opening_time,
    DROP COLUMN closing_time,
    ADD COLUMN opening_hours TEXT,
    ADD COLUMN contact_phone TEXT,
    ADD COLUMN contact_email TEXT,
    ADD COLUMN contact_website TEXT;