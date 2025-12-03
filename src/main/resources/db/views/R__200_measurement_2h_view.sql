DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_2h_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_2h_view
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('2 hours', bucket) AS bucket,
    average(rollup(stats)) AS avg,
    MIN(min) AS min,
    MAX(max) AS max,
    num_vals(rollup(stats)) AS count,
    stddev(rollup(stats)) AS stddev,
    rollup(stats) AS stats
FROM marlin.measurement_1h_view
GROUP BY 1,2,3,4
WITH NO DATA;


ALTER MATERIALIZED VIEW marlin.measurement_2h_view
    SET (timescaledb.materialized_only = false);
