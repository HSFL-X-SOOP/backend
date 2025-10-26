ALTER TABLE Location
    ADD COLUMN description TEXT,
    ADD COLUMN address TEXT,
    ADD COLUMN opening_time TIME,
    ADD COLUMN closing_time TIME,
    ADD COLUMN created_at TIMESTAMP DEFAULT now();

CREATE TABLE marlin.location_image (
        location_id BIGINT PRIMARY KEY REFERENCES Location(id) ON DELETE CASCADE,
        image BYTEA NOT NULL,
        uploaded_at TIMESTAMP DEFAULT now()
);
