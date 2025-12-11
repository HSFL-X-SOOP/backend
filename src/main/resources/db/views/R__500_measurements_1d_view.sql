DROP MATERIALIZED VIEW IF EXISTS marlin.measurement_1d_view CASCADE;

CREATE MATERIALIZED VIEW marlin.measurement_1d_view
    WITH (timescaledb.continuous) AS
SELECT sensor_id,
       type_id,
       location_id,
       public.time_bucket('1 day', bucket)   AS bucket,
       public.average(rollup(stats))         AS avg,
       MIN(min)                              AS min,
       MAX(max)                              AS max,
       public.num_vals(public.rollup(stats)) AS count,
       public.stddev(public.rollup(stats))   AS stddev,
       public.rollup(stats)                  AS stats
FROM marlin.measurement_12h_view
GROUP BY 1, 2, 3, 4
WITH NO DATA;

ALTER MATERIALIZED VIEW marlin.measurement_1d_view
    SET (timescaledb.materialized_only = false);

-- Refresh the 1d view every 6 hours
SELECT public.add_continuous_aggregate_policy('marlin.measurement_1d_view',
                                              start_offset => INTERVAL '1 month', -- Look back 1 month (ensure monthly stats are solid)
                                              end_offset => INTERVAL '1 day', -- Don't materialize today yet (wait until tomorrow)
                                              schedule_interval => INTERVAL '6 hours');
