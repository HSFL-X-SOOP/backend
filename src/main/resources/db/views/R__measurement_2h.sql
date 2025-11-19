DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_2h CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_2h
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('2 hours', bucket) AS bucket,
    average(rollup(stats_1h)) AS avg_2h,
    MIN(min_1h) AS min_2h,
    MAX(max_1h) AS max_2h,
    num_vals(rollup(stats_1h)) AS count_2h,
    stddev(rollup(stats_1h)) AS stddev_2h,
    rollup(stats_1h) AS stats_2h
FROM marlin.measurement_1h
GROUP BY 1,2,3,4
WITH NO DATA;


ALTER MATERIALIZED VIEW marlin.measurement_2h
    SET (timescaledb.materialized_only = false);
