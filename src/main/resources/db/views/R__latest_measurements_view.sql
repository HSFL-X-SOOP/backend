DROP VIEW IF EXISTS marlin.latest_measurements_view;

CREATE VIEW marlin.latest_measurements_view AS
WITH latest AS (
    SELECT DISTINCT ON (m.location_id, m.type_id)
        m.sensor_id,
        m.type_id,
        m.location_id,
        m.time,
        m.value
    FROM marlin.measurement AS m
    WHERE m.time >= NOW() - INTERVAL '2 hour'
    ORDER BY m.location_id, m.type_id, m.time DESC
)
SELECT
    l.id AS location_id,
    l.name AS location_name,

    ST_Y(l.coordinates::geometry) AS latitude,
    ST_X(l.coordinates::geometry) AS longitude,

    s.id AS sensor_id,
    s.name AS sensor_name,
    s.description AS sensor_description,
    s.is_moving AS sensor_is_moving,

    mt.id AS type_id,
    mt.name AS type_name,
    mt.description AS type_description,
    mt.unit_name,
    mt.unit_symbol,
    mt.unit_definition,

    lm.time AS meas_time,
    lm.value AS meas_value

FROM latest AS lm
         JOIN marlin.location l ON lm.location_id = l.id
         JOIN marlin.sensor s ON lm.sensor_id = s.id
         JOIN marlin.measurementtype mt ON lm.type_id = mt.id

ORDER BY l.id;