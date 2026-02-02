BEGIN;

-- ==================================================================
-- STAFF USERS SEED DATA
-- ==================================================================
-- Password for all users: "DSi@2026Agentic"
-- Hash generated using BCrypt with strength 10

WITH ts AS (SELECT now() AS now_ts)
INSERT
INTO app_user
(row_version,
 created_at,
 updated_at,
 created_by,
 updated_by,
 username,
 email,
 password_hash,
 full_name,
 role,
 active,
 email_verified)
VALUES
  -- ADMIN USER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'admin',
   'ornob011+10@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'System Administrator',
    'ADMIN',
    TRUE,
   TRUE),

  -- SUPERVISOR USER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'supervisor1',
   'ornob011+11@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'Jane Supervisor',
    'SUPERVISOR',
    TRUE,
   TRUE),

  -- AGENT USERS (multiple for different queues)
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'agent_billing',
   'ornob011+12@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'Bob Billing Agent',
    'AGENT',
    TRUE,
   TRUE),

  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'agent_tech',
   'ornob011+13@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'Alice Technical Agent',
    'AGENT',
    TRUE,
   TRUE),

  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'agent_security',
   'ornob011+14@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'Charlie Security Agent',
    'AGENT',
    TRUE,
   TRUE),

  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SYSTEM_SEED',
   'SYSTEM_SEED',
   'agent_general',
   'ornob011+15@gmail.com',
   '$2a$10$TXPqY8Kc5qwJZ8Hd.vLx7eXxGJ3QC8aF.WZvJhKLmN9pQRsTuVwWy',
    'Diana General Agent',
    'AGENT',
    TRUE,
   TRUE)
ON CONFLICT (username)
  DO UPDATE SET email          = EXCLUDED.email,
                password_hash  = EXCLUDED.password_hash,
                full_name      = EXCLUDED.full_name,
                role           = EXCLUDED.role,
                active         = EXCLUDED.active,
                email_verified = EXCLUDED.email_verified,
                updated_at     = (SELECT now_ts FROM ts),
                updated_by     = 'SYSTEM_SEED'
WHERE app_user.email IS DISTINCT FROM EXCLUDED.email
   OR app_user.password_hash IS DISTINCT FROM EXCLUDED.password_hash
   OR app_user.full_name IS DISTINCT FROM EXCLUDED.full_name
   OR app_user.role IS DISTINCT FROM EXCLUDED.role
   OR app_user.active IS DISTINCT FROM EXCLUDED.active
   OR app_user.email_verified IS DISTINCT FROM EXCLUDED.email_verified;

COMMIT;
