CREATE TABLE IF NOT EXISTS marlin.notification_locations
(
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES marlin.location (id) ON DELETE CASCADE,
    notification_title TEXT,
    notification_text TEXT,
    created_by BIGINT NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);