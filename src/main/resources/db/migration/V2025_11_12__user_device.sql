CREATE TABLE IF NOT EXISTS marlin.user_device
(
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    fcm_token TEXT,
    user_id BIGINT NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE
);