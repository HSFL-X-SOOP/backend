DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_1d_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_1d_view
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('1 day', bucket) AS bucket,
    average(rollup(stats)) AS avg,
    MIN(min) AS min,
    MAX(max) AS max,
    num_vals(rollup(stats)) AS count,
    stddev(rollup(stats)) AS stddev,
    rollup(stats) AS stats
FROM marlin.measurement_12h_view
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_1d_view
    SET (timescaledb.materialized_only = false);

-- Refresh the 1d view every 6 hours
SELECT add_continuous_aggregate_policy('marlin.measurement_1d_view',
       start_offset => INTERVAL '1 month',   -- Look back 1 month (ensure monthly stats are solid)
       end_offset   => INTERVAL '1 day',     -- Don't materialize today yet (wait until tomorrow)
       schedule_interval => INTERVAL '6 hours');
