ALTER TABLE marlin.user_profile
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);


COMMENT
ON COLUMN marlin.user_profile.first_name IS 'User''s first name (Vorname)';
COMMENT
ON COLUMN marlin.user_profile.last_name IS 'User''s last name (Nachname)';
