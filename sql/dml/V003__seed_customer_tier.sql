BEGIN;

INSERT INTO customer_tier (code, display_name, active)
VALUES ('FREE', 'Free', TRUE),
       ('STANDARD', 'Standard', TRUE),
       ('PREMIUM', 'Premium', TRUE),
       ('ENTERPRISE', 'Enterprise', TRUE)
ON CONFLICT (code)
  DO UPDATE
  SET display_name = EXCLUDED.display_name,
      active       = EXCLUDED.active
WHERE customer_tier.display_name IS DISTINCT FROM EXCLUDED.display_name
   OR customer_tier.active IS DISTINCT FROM EXCLUDED.active;

COMMIT;
