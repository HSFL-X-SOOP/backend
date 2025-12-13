-- Fix needed cause; aggregates are not working for location_id 1, due to the old measurement type
-- switch to the new measurement type, WTemp -> Temperature, water

UPDATE marlin.measurement
SET type_id = 1603253327 -- New ID ("Temperature, water")
WHERE type_id = 82949323; -- Old ID ("WTemp")

DELETE
FROM marlin.measurementtype
WHERE id = 82949323;
-- Old ID ("WTemp")
