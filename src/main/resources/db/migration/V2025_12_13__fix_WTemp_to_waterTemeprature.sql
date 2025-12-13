-- Fix needed cause; aggregates are not working for location_id 1, due to the old measurement type
-- switch to the new measurement type, WTemp -> Temperature, water

-- Remove rows with the old Id, if a row with the "new Id already exists
-- at the exact same timestamp and sensor.
DELETE
FROM marlin.measurement old
WHERE type_id = 82949323 -- The Old ID
  AND EXISTS (SELECT 1
              FROM marlin.measurement new
              WHERE new.type_id = 1603253327 -- The New ID
                AND new.sensor_id = old.sensor_id
                AND new.time = old.time);

UPDATE marlin.measurement
SET type_id = 1603253327 -- New ID ("Temperature, water")
WHERE type_id = 82949323; -- Old ID ("WTemp")

DELETE
FROM marlin.measurementtype
WHERE id = 82949323;
-- Old ID ("WTemp")
