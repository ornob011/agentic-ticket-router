BEGIN;

WITH ts AS (SELECT now() AS now_ts)
INSERT
INTO model_registry
(row_version,
 created_at,
 updated_at,
 model_tag,
 model_name,
 version,
 description,
 base_model,
 training_method,
 quantization,
 artifact_path,
 active,
 is_default,
 activated_at,
 deactivated_at,
 activated_by_id,
 performance_metrics,
 model_config)
VALUES (0,
        (SELECT now_ts FROM ts),
        (SELECT now_ts FROM ts),
        'ollama-phi',
        'Phi (Ollama)',
        'latest',
        'Seeded default model for local Ollama inference (ollama run phi)',
        'phi',
        NULL,
        NULL,
        'ollama://phi',
        TRUE,
        TRUE,
        (SELECT now_ts FROM ts),
        NULL,
        NULL,
        NULL,
        NULL)
ON CONFLICT (model_tag)
  DO UPDATE SET model_name      = EXCLUDED.model_name,
                version         = EXCLUDED.version,
                description     = EXCLUDED.description,
                base_model      = EXCLUDED.base_model,
                training_method = EXCLUDED.training_method,
                quantization    = EXCLUDED.quantization,
                artifact_path   = EXCLUDED.artifact_path,
                active          = EXCLUDED.active,
                is_default      = EXCLUDED.is_default,

                activated_at    =
                  CASE
                    WHEN EXCLUDED.active AND NOT model_registry.active
                      THEN (SELECT now_ts FROM ts)
                    ELSE model_registry.activated_at
                    END,

                deactivated_at  =
                  CASE
                    WHEN NOT EXCLUDED.active AND model_registry.active
                      THEN (SELECT now_ts FROM ts)
                    WHEN EXCLUDED.active
                      THEN NULL
                    ELSE model_registry.deactivated_at
                    END,

                updated_at      = (SELECT now_ts FROM ts)
WHERE model_registry.model_name IS DISTINCT FROM EXCLUDED.model_name
   OR model_registry.version IS DISTINCT FROM EXCLUDED.version
   OR model_registry.description IS DISTINCT FROM EXCLUDED.description
   OR model_registry.base_model IS DISTINCT FROM EXCLUDED.base_model
   OR model_registry.training_method IS DISTINCT FROM EXCLUDED.training_method
   OR model_registry.quantization IS DISTINCT FROM EXCLUDED.quantization
   OR model_registry.artifact_path IS DISTINCT FROM EXCLUDED.artifact_path
   OR model_registry.active IS DISTINCT FROM EXCLUDED.active
   OR model_registry.is_default IS DISTINCT FROM EXCLUDED.is_default;

COMMIT;
