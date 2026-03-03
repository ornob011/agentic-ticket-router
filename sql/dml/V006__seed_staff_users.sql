BEGIN;

-- ==================================================================
-- STAFF USERS SEED DATA
-- ==================================================================
-- Password for all users: "AIClub123"

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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
   '3488d8f14a90213497d82feae0bc14eee52aa0ced99ec60870a9b5e5a4d9d3810c9ec3b9965c904836ac93d8480f176f659658dcd3e49f40e990604b7e0c0efd6b48f130b46fbf55f828628c63d69ac56c5782e7126ee52dbbcd8d2d4445c0e28ac1d58704d1acd7a0e59b655de72333f5a885a0c84f0e50b2effe8dbad2edde1c1bdf5ab05199a4539123cfde46e0d4eb8fbf84799d1fac03b18fd88454342f',
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
