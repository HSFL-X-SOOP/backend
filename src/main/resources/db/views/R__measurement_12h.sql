DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_12h CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_12h
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('1 day', bucket) AS bucket,
    average(rollup(stats_6h)) AS avg_12h,
    MIN(min_6h) AS min_12h,
    MAX(max_6h) AS max_12h,
    num_vals(rollup(stats_6h)) AS count_12h,
    stddev(rollup(stats_6h)) AS stddev_12h,
    rollup(stats_6h) AS stats_12h
FROM marlin.measurement_6h
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_12h
    SET (timescaledb.materialized_only = false);
