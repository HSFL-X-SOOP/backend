DROP VIEW IF EXISTS marlin.user_view;

CREATE VIEW marlin.user_view AS
SELECT u.id,
       u.first_name,
       u.last_name,
       u.email,
       u.verified,
       u.role          AS authority_role,
       u.password,
       u.stripe_customer_id,
       up.language,
       up.role         AS activity_roles,
       up.measurement_system,
       hml.location_id AS assigned_location_id,
       u.created_at    AS user_created_at,
       u.updated_at    AS user_updated_at,
       up.created_at   AS profile_created_at,
       up.updated_at   AS profile_updated_at,
       (SELECT s.status
        FROM marlin.subscription s
        WHERE s.user_id = u.id
          AND s.subscription_type = 'APP_NOTIFICATION'
          AND s.status IN ('ACTIVE', 'TRIALING', 'PAST_DUE')
        LIMIT 1) AS notification_subscription_status,
       (SELECT s.status
        FROM marlin.subscription s
        WHERE s.user_id = u.id
          AND s.subscription_type = 'API_ACCESS'
          AND s.status IN ('ACTIVE', 'TRIALING', 'PAST_DUE')
        LIMIT 1) AS api_subscription_status
FROM marlin.user u
         LEFT JOIN marlin.user_profile up ON up.user_id = u.id
         LEFT JOIN marlin.harbor_master_location hml ON hml.user_id = u.id;
