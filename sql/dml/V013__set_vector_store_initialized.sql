\set ON_ERROR_STOP on

BEGIN;

UPDATE global_config
SET
    map_value = '{"INITIALIZED": true}'::jsonb,
    updated_at = now()
WHERE config_key = 'VECTOR_STORE_INITIALIZED';

COMMIT;
