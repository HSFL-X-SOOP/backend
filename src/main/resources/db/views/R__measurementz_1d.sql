DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_1d CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_1d
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('1 day', bucket) AS bucket,
    average(rollup(stats_12h)) AS avg_1d,
    MIN(min_12h) AS min_1d,
    MAX(max_12h) AS max_1d,
    num_vals(rollup(stats_12h)) AS count_1d,
    stddev(rollup(stats_12h)) AS stddev_1d,
    rollup(stats_12h) AS stats_1d
FROM marlin.measurement_12h
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_1d
    SET (timescaledb.materialized_only = false);
