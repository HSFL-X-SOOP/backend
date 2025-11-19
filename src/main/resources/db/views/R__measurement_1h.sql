DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_1h CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_1h
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('1 hour', time) AS bucket,
    avg(value) AS avg_1h,
    min(value) AS min_1h,
    max(value) AS max_1h,
    count(value) AS count_1h,
    stats_agg(value) AS stats_1h
FROM marlin.measurement
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_1h
    SET (timescaledb.materialized_only = false);
