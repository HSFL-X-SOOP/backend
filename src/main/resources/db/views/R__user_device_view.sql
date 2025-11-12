DROP VIEW IF EXISTS marlin.user_device_view;

CREATE VIEW marlin.user_device_view AS
SELECT ud.id,
       ud.device_id,
       ud.fcm_token,
       ud.user_id
FROM marlin.user_device ud