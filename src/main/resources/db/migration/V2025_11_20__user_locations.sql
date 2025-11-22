CREATE TABLE IF NOT EXISTS marlin.user_locations
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES marlin.location (id) ON DELETE CASCADE,
    sent_harbor_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, location_id)
);