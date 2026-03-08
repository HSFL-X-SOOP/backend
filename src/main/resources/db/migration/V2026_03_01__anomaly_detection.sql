CREATE TABLE IF NOT EXISTS marlin.anomalies
(
    id BIGSERIAL PRIMARY KEY,
    measurement_sensor_id BIGINT,
    measurement_type_id BIGINT,
    measurement_time TIMESTAMPTZ,

    FOREIGN KEY (measurement_sensor_id, measurement_type_id, measurement_time)
    REFERENCES marlin.Measurement(sensor_id, type_id, time)
    ON DELETE CASCADE
);