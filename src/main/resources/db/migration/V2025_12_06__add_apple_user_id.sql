ALTER TABLE marlin.user
    ADD COLUMN apple_user_id TEXT UNIQUE;

CREATE INDEX idx_user_apple_user_id ON marlin.user(apple_user_id);
