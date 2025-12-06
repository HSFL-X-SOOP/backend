DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_1h_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_1h_view
            WITH (timescaledb.continuous) AS
SELECT
    sensor_id,
    type_id,
    location_id,
    time_bucket('1 hour', time) AS bucket,
    avg(value) AS avg,
    min(value) AS min,
    max(value) AS max,
    count(value) AS count,
    stats_agg(value) AS stats
FROM marlin.measurement
GROUP BY 1,2,3,4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_1h_view
    SET (timescaledb.materialized_only = false);

-- Refresh the 1h view every 30 minutes
SELECT add_continuous_aggregate_policy('marlin.measurement_1h_view',
       start_offset => INTERVAL '3 days',    -- Re-calculate last 3 days to catch late sensor uploads
       end_offset   => INTERVAL '1 hour',    -- Don't materialize the current incomplete hour
       schedule_interval => INTERVAL '30 minutes');
