BEGIN;

WITH ts AS (SELECT now() AS now_ts)
INSERT
INTO global_config
(
  row_version,
  created_at,
  updated_at,
  config_key,
  map_value
)
VALUES
  (
    0,
    (SELECT now_ts FROM ts),
    (SELECT now_ts FROM ts),
    'VECTOR_STORE_INITIALIZED',
    '{"initialized": false}'::jsonb
  ),
  (
    0,
    (SELECT now_ts FROM ts),
    (SELECT now_ts FROM ts),
    'VECTOR_STORE_VERSION',
    '{"version": "1.0"}'::jsonb
  ),
  (
    0,
    (SELECT now_ts FROM ts),
    (SELECT now_ts FROM ts),
    'KB_SEED_VERSION',
    '{"version": "1.0"}'::jsonb
  )
ON CONFLICT (config_key)
  DO UPDATE SET
              map_value   = EXCLUDED.map_value,
              updated_at  = (SELECT now_ts FROM ts)
WHERE global_config.map_value IS DISTINCT FROM EXCLUDED.map_value;

COMMIT;
