-- Create CTE to find the earliest measurement time for each location
WITH LocationMinTime AS (SELECT location_id,
                                MIN(time) AS earliest_measurement_time
                         FROM marlin.Measurement
                         GROUP BY location_id)

-- Update Location created_at timestamps to match the earliest measurement time
UPDATE marlin.Location
SET created_at = LocationMinTime.earliest_measurement_time
FROM LocationMinTime
WHERE marlin.Location.id = LocationMinTime.location_id
  -- Only update if it's older
  AND LocationMinTime.earliest_measurement_time < marlin.Location.created_at;