DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_12h_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_12h_view
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('12 hours', bucket) AS bucket,
    average(rollup(stats)) AS avg,
    MIN(min) AS min,
    MAX(max) AS max,
    num_vals(rollup(stats)) AS count,
    stddev(rollup(stats)) AS stddev,
    rollup(stats) AS stats
FROM marlin.measurement_6h_view
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_12h_view
    SET (timescaledb.materialized_only = false);

-- Refresh the 12h view every 2 hours
SELECT add_continuous_aggregate_policy('marlin.measurement_12h_view',
       start_offset => INTERVAL '14 days',   -- Look back 2 weeks
       end_offset   => INTERVAL '12 hours',  -- Don't materialize the current incomplete 12-hour block
       schedule_interval => INTERVAL '2 hours');