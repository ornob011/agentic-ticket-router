\set ON_ERROR_STOP on

BEGIN;

UPDATE model_registry
SET active         = FALSE,
    is_default     = FALSE,
    deactivated_at = now(),
    updated_at     = now()
WHERE model_tag <> 'ollama-qwen3:4b-instruct';

INSERT INTO model_registry
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
        now(),
        now(),
        'ollama-qwen3:4b-instruct',
        'Qwen 3 4B Instruct (Ollama)',
        '4b',
        'Default model for local Ollama inference (ollama run qwen3:4b-instruct) with maximum determinism (temperature=0.0)',
        'qwen3:4b-instruct',
        NULL,
        NULL,
        'ollama://qwen3:4b-instruct',
        TRUE,
        TRUE,
        now(),
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
                activated_at    = COALESCE(model_registry.activated_at, now()),
                deactivated_at  = CASE
                                    WHEN EXCLUDED.active THEN NULL
                                    ELSE model_registry.deactivated_at
                  END,
                updated_at      = now();

COMMIT;
