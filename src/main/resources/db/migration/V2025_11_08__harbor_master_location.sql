CREATE TABLE marlin.harbor_master_location (
       user_id BIGINT PRIMARY KEY REFERENCES marlin.user (id) ON DELETE CASCADE,
       location_id BIGINT NOT NULL REFERENCES marlin.location (id) ON DELETE CASCADE,
       assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       assigned_by BIGINT REFERENCES marlin.user (id) ON DELETE SET NULL
);

CREATE INDEX idx_harbor_master_location_id ON marlin.harbor_master_location(location_id);