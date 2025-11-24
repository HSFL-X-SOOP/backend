CREATE TABLE IF NOT EXISTS marlin.user_device
(
    id BIGSERIAL PRIMARY KEY,
    fcm_token TEXT,
    user_id BIGINT NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,

    UNIQUE (user_id, fcm_token)
);