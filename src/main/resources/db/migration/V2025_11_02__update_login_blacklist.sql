ALTER TABLE marlin.login_blacklist
    ALTER COLUMN ip_address DROP NOT NULL,
    ALTER COLUMN blocked_until DROP NOT NULL,
    ADD COLUMN note TEXT;

COMMENT ON COLUMN marlin.login_blacklist.ip_address IS 'IP address of the blocked login attempt. Nullable for cases where IP is not available or not relevant to the blacklist reason.';
COMMENT ON COLUMN marlin.login_blacklist.blocked_until IS 'Timestamp when the block expires. NULL means the user is indefinitely blocked until manually unblocked.';
COMMENT ON COLUMN marlin.login_blacklist.note IS 'Optional note explaining the reason for blacklisting the user.';
