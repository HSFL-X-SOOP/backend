DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_6h_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_6h_view
    WITH (timescaledb.continuous) AS
SELECT sensor_id,
       type_id,
       location_id,
       public.time_bucket('6 hours', bucket) AS bucket,
       public.average(public.rollup(stats))  AS avg,
       MIN(min)                              AS min,
       MAX(max)                              AS max,
       public.num_vals(public.rollup(stats)) AS count,
       public.stddev(public.rollup(stats))   AS stddev,
       public.rollup(stats)                  AS stats
FROM marlin.measurement_2h_view
GROUP BY 1, 2, 3, 4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_6h_view
    SET (timescaledb.materialized_only = false);

-- Refresh the 6h view every hour
SELECT public.add_continuous_aggregate_policy('marlin.measurement_6h_view',
                                              start_offset => INTERVAL '7 days', -- Look back 7 days (ensure weekly trends are fixed)
                                              end_offset => INTERVAL '6 hours', -- Don't materialize the current incomplete 6-hour block
                                              schedule_interval => INTERVAL '1 hour');