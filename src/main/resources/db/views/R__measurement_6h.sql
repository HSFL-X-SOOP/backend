DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_6h CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_6h
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('6 hours', bucket) AS bucket,
    average(rollup(stats_2h)) AS avg_6h,
    MIN(min_2h) AS min_6h,
    MAX(max_2h) AS max_6h,
    num_vals(rollup(stats_2h)) AS count_6h,
    stddev(rollup(stats_2h)) AS stddev_6h,
    rollup(stats_2h) AS stats_6h
FROM marlin.measurement_2h
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_6h
    SET (timescaledb.materialized_only = false);
