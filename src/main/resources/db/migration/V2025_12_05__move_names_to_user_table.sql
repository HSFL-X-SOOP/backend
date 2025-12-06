ALTER TABLE marlin.user
    ADD COLUMN first_name TEXT,
    ADD COLUMN last_name  TEXT;

UPDATE marlin.user u
SET first_name = up.first_name,
    last_name  = up.last_name
FROM marlin.user_profile up
WHERE u.id = up.user_id
  AND (up.first_name IS NOT NULL OR up.last_name IS NOT NULL);

DROP VIEW IF EXISTS marlin.user_view;

ALTER TABLE marlin.user_profile
    DROP COLUMN IF EXISTS first_name,
    DROP COLUMN IF EXISTS last_name;
