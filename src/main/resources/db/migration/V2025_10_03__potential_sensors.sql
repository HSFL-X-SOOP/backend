CREATE TABLE marlin.potential_sensor (
        id BIGINT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        is_active BOOLEAN NOT NULL DEFAULT FALSE
);