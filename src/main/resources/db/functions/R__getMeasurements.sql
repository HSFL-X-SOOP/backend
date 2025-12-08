CREATE OR REPLACE FUNCTION marlin.get_measurements(
    p_time_range text,
    p_location_id bigint,
    p_sensor_id bigint DEFAULT NULL,
    p_type_id bigint DEFAULT NULL
)
    RETURNS TABLE
            (
                sensor_id   bigint,
                type_id     bigint,
                location_id bigint,
                bucket      timestamp with time zone,
                avg         double precision,
                min         double precision,
                max         double precision,
                count       bigint,
                stddev      double precision
            )
    LANGUAGE plpgsql
AS
$$
DECLARE
    sql TEXT;
BEGIN
    -- Choose correct view based on time range
    CASE lower(p_time_range)
        WHEN '24h' THEN sql := '
                SELECT sensor_id, type_id, location_id, time AS bucket,
                       value AS avg, value AS min, value AS max, 
                       1::bigint AS count,
                       NULL::double precision AS stddev
                FROM marlin.measurement
                WHERE time >= NOW() - INTERVAL ''24 hours''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY time DESC';
        WHEN '48h' THEN sql := '
                SELECT sensor_id, type_id, location_id, time AS bucket,
                       value AS avg, value AS min, value AS max, 
                       1::bigint AS count,
                       NULL::double precision AS stddev
                FROM marlin.measurement
                WHERE time >= NOW() - INTERVAL ''48 hours''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY time DESC';

        WHEN '7d' THEN sql := '
                SELECT sensor_id, type_id, location_id, bucket,
                       avg, min, max, count, stddev
                FROM marlin.measurement_2h_view
                WHERE bucket >= NOW() - INTERVAL ''7 days''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY bucket DESC';

        WHEN '30d' THEN sql := '
                SELECT sensor_id, type_id, location_id, bucket,
                       avg, min, max, count, stddev
                FROM marlin.measurement_6h_view
                WHERE bucket >= NOW() - INTERVAL ''30 days''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY bucket DESC';

        WHEN '90d' THEN sql := '
                SELECT sensor_id, type_id, location_id, bucket,
                       avg, min, max, count, stddev
                FROM marlin.measurement_12h_view
                WHERE bucket >= NOW() - INTERVAL ''90 days''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY bucket DESC';

        WHEN '180d' THEN sql := '
                SELECT sensor_id, type_id, location_id, bucket,
                       avg, min, max, count, stddev
                FROM marlin.measurement_1d_view
                WHERE bucket >= NOW() - INTERVAL ''180 days''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY bucket DESC';

        WHEN '1y' THEN sql := '
                SELECT sensor_id, type_id, location_id, bucket,
                       avg, min, max, count, stddev
                FROM marlin.measurement_1d_view
                WHERE bucket >= NOW() - INTERVAL ''1 year''
                  AND location_id = $1
                  AND ($2 IS NULL OR sensor_id = $2)
                  AND ($3 IS NULL OR type_id = $3)
                ORDER BY bucket DESC';

        ELSE RAISE EXCEPTION 'Invalid time range: %', p_time_range;
        END CASE;

    -- Execute dynamic SQL passing all 3 parameters
    RETURN QUERY EXECUTE sql USING p_location_id, p_sensor_id, p_type_id;
END;
$$;
