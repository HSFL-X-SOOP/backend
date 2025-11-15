CREATE TABLE IF NOT EXISTS marlin.notification_measurement_rule
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES marlin.user (id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES marlin.location (id) ON DELETE CASCADE,
    measurement_type_id BIGINT NOT NULL REFERENCES marlin.measurementtype (id) ON DELETE CASCADE,
    operator VARCHAR(2) NOT NULL,
    valu DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);