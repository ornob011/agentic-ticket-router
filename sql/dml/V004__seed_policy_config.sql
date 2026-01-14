BEGIN;

WITH ts AS (SELECT now() AS now_ts)
INSERT
INTO policy_config
(row_version,
 created_at,
 updated_at,
 config_key,
 config_value,
 value_type,
 description,
 active,
 default_value,
 min_value,
 max_value)
VALUES
  -- DOUBLE
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'AUTO_ROUTE_THRESHOLD',
   '0.70',
   'DOUBLE'::config_value_type,
   'confidence >= this => allow auto actions',
   TRUE,
   '0.70',
   '0.00',
   '1.00'),

  -- DOUBLE
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'CRITICAL_MIN_CONF',
   '0.85',
   'DOUBLE'::config_value_type,
   'if priority=CRITICAL and confidence < this => HUMAN_REVIEW',
   TRUE,
   '0.85',
   '0.00',
   '1.00'),

  -- INTEGER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'ROUTER_REPAIR_MAX_RETRIES',
   '2',
   'INTEGER'::config_value_type,
   'max JSON repair retries before HUMAN_REVIEW',
   TRUE,
   '2',
   '0',
   '5'),

  -- INTEGER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'SLA_ASSIGNED_HOURS_HIGH',
   '24',
   'INTEGER'::config_value_type,
   'hours before reminding when assigned/in-progress high priority',
   TRUE,
   '24',
   '1',
   '168'),

  -- INTEGER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'WAITING_CUSTOMER_REMINDER_HOURS',
   '48',
   'INTEGER'::config_value_type,
   'hours before reminding customer when waiting on customer response',
   TRUE,
   '48',
   '1',
   '720'),

  -- INTEGER
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'INACTIVITY_AUTO_CLOSE_DAYS',
   '14',
   'INTEGER'::config_value_type,
   'days of inactivity before auto-close workflow',
   TRUE,
   '14',
   '1',
   '365'),

  -- LONG
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'MAX_ATTACHMENT_BYTES',
   '52428800',
   'LONG'::config_value_type,
   'max attachment size in bytes',
   TRUE,
   '52428800',
   '0',
   '214748364800'),

  -- BOOLEAN
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'AUTO_CLOSE_ENABLED',
   'true',
   'BOOLEAN'::config_value_type,
   'enable inactivity auto-close workflow',
   TRUE,
   'true',
   NULL,
   NULL),

  -- STRING
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'DEFAULT_QUEUE',
   'GENERAL',
   'STRING'::config_value_type,
   'default routing queue key',
   TRUE,
   'GENERAL',
   NULL,
   NULL),

  -- JSON
  (0,
   (SELECT now_ts FROM ts),
   (SELECT now_ts FROM ts),
   'ROUTER_MODEL_PARAMS',
   '{"temperature":0.2,"top_p":1.0}',
   'JSON'::config_value_type,
   'model parameters for router inference',
   TRUE,
   '{"temperature":0.2,"top_p":1.0}',
   NULL,
   NULL)
ON CONFLICT (config_key)
  DO UPDATE SET config_value  = EXCLUDED.config_value,
                value_type    = EXCLUDED.value_type,
                description   = EXCLUDED.description,
                active        = EXCLUDED.active,
                default_value = EXCLUDED.default_value,
                min_value     = EXCLUDED.min_value,
                max_value     = EXCLUDED.max_value,
                updated_at    = (SELECT now_ts FROM ts)
WHERE policy_config.config_value IS DISTINCT FROM EXCLUDED.config_value
   OR policy_config.value_type IS DISTINCT FROM EXCLUDED.value_type
   OR policy_config.description IS DISTINCT FROM EXCLUDED.description
   OR policy_config.active IS DISTINCT FROM EXCLUDED.active
   OR policy_config.default_value IS DISTINCT FROM EXCLUDED.default_value
   OR policy_config.min_value IS DISTINCT FROM EXCLUDED.min_value
   OR policy_config.max_value IS DISTINCT FROM EXCLUDED.max_value;

COMMIT;
