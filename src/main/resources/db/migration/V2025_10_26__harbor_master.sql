ALTER TYPE marlin.user_authority_role ADD VALUE 'HARBOR_MASTER' BEFORE 'USER';

ALTER TYPE marlin.user_activity_role RENAME TO user_activity_role_old;

CREATE TYPE marlin.user_activity_role AS ENUM ('SWIMMER', 'SAILOR', 'FISHERMAN');

UPDATE marlin.user_profile
SET role = array_remove(role, 'HARBOR_MASTER'::marlin.user_activity_role_old)
WHERE 'HARBOR_MASTER' = ANY(role);

-- Update the column to use the new enum type
ALTER TABLE marlin.user_profile
ALTER COLUMN role TYPE marlin.user_activity_role[]
    USING role::text[]::marlin.user_activity_role[];

DROP TYPE marlin.user_activity_role_old;