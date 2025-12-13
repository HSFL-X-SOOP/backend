CREATE OR REPLACE FUNCTION marlin.get_enriched_measurements(
    p_time_range text,
    p_location_id bigint,
    p_sensor_id bigint DEFAULT NULL,
    p_type_id bigint DEFAULT NULL
)
    RETURNS TABLE
            (
                location_id        bigint,
                location_name      text,
                latitude           double precision,
                longitude          double precision,
                sensor_id          bigint,
                sensor_name        text,
                sensor_description text,
                sensor_is_moving   boolean,
                type_id            bigint,
                type_name          text,
                type_description   text,
                unit_name          text,
                unit_symbol        text,
                unit_definition    text,
                bucket             timestamp with time zone,
                avg                double precision,
                min                double precision,
                max                double precision,
                count              bigint,
                stddev             double precision
            )
    STABLE
    LANGUAGE plpgsql
AS
$$
BEGIN
    RETURN QUERY
        WITH raw_data AS (SELECT * FROM marlin.get_measurements(p_time_range, p_location_id, p_sensor_id, p_type_id))
        SELECT rd.location_id,
               l.name::text                                   AS location_name,
               marlin.ST_Y(l.coordinates::geometry)                  AS latitude,
               marlin.ST_X(l.coordinates::geometry)                  AS longitude,
               rd.sensor_id,
               s.name::text                                   AS sensor_name,
               s.description::text                            AS sensor_description,
               s.is_moving                                    AS sensor_is_moving,
               rd.type_id,
               mt.name::text                                  AS type_name,
               mt.description::text                           AS type_description,
               mt.unit_name::text                             AS unit_name,
               mt.unit_symbol::text                           AS unit_symbol,
               mt.unit_definition::text                       AS unit_definition,
               rd.bucket,
               ROUND(rd.avg::numeric, 2)::double precision    AS avg,
               ROUND(rd.min::numeric, 2)::double precision    AS min,
               ROUND(rd.max::numeric, 2)::double precision    AS max,
               rd.count,
               ROUND(rd.stddev::numeric, 2)::double precision AS stddev
        FROM raw_data rd
                 JOIN marlin.location l ON rd.location_id = l.id
                 JOIN marlin.sensor s ON rd.sensor_id = s.id
                 JOIN marlin.measurementtype mt ON rd.type_id = mt.id
        ORDER BY rd.bucket DESC;
END;
$$;