ALTER TABLE marlin.location_image
    RENAME COLUMN image TO data;

ALTER TABLE marlin.location_image
    ADD COLUMN content_type VARCHAR(100);