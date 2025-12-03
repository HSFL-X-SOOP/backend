ALTER TABLE marlin.notification_measurement_rule
    ADD last_notified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD last_state BOOLEAN NOT NULL DEFAULT FALSE