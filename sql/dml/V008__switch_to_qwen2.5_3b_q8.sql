\set ON_ERROR_STOP on

BEGIN;

UPDATE model_registry
SET active = FALSE,
    is_default = FALSE,
    deactivated_at = now(),
    updated_at = now()
WHERE is_default = TRUE
  AND model_tag <> 'ollama-qwen2.5-3b-q8';

INSERT INTO model_registry
(row_version, created_at, updated_at, model_tag, model_name, version,
 description, base_model, training_method, quantization, artifact_path,
 active, is_default, activated_at, deactivated_at, activated_by_id,
 performance_metrics, model_config)
VALUES (0, now(), now(),
        'ollama-qwen2.5-3b-q8',
        'Qwen 2.5 3B Instruct (Ollama)',
        '3b',
        'Default model for local Ollama inference (ollama run qwen2.5-3b-instruct:Q8_0) optimized for fast, accurate ticket analysis with maximum determinism (temperature=0.0).',
        'qwen2.5-3b-instruct',
        NULL,
        'Q8_0',
        'ollama://qwen2.5-3b-instruct:Q8_0',
        TRUE,
        TRUE,
        now(),
        NULL,
        NULL,
        NULL,
        '{
          "temperature": 0.0,
          "top_p": 0.9,
          "num_ctx": 32768,
          "format": "json"
        }'::jsonb)
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
                updated_at      = now(),
                model_config    = EXCLUDED.model_config;

COMMIT;
