-- DEFAULT DISTANCE OF 5 METERS
CREATE OR REPLACE FUNCTION marlin.get_or_create_location(lon DOUBLE PRECISION, lat DOUBLE PRECISION, distance INTEGER DEFAULT 5)
    RETURNS TABLE
            (
                location_id BIGINT
            )
AS
$$
DECLARE
    found_id BIGINT;
BEGIN
    -- Look for existing location within radius
    SELECT id
    INTO found_id
    FROM marlin.location
    WHERE marlin.ST_DWithin(coordinates, marlin.ST_SetSRID(marlin.ST_MakePoint(lon, lat), 4326), distance)
    LIMIT 1;

    IF found_id IS NOT NULL THEN
        RETURN QUERY SELECT found_id;
    ELSE
        RETURN QUERY
            INSERT INTO marlin.location (coordinates)
                VALUES (marlin.ST_SetSRID(marlin.ST_MakePoint(lon, lat), 4326))
                RETURNING id;
    END IF;
END;
$$ LANGUAGE plpgsql;