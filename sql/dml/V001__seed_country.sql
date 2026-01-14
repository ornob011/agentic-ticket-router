BEGIN;

INSERT INTO country (iso2, name, active)
VALUES ('BD', 'Bangladesh', TRUE),
       ('US', 'United States', TRUE),
       ('GB', 'United Kingdom', TRUE),
       ('CA', 'Canada', TRUE),
       ('AU', 'Australia', TRUE)
ON CONFLICT (iso2)
  DO UPDATE
  SET name   = EXCLUDED.name,
      active = EXCLUDED.active
WHERE country.name IS DISTINCT FROM EXCLUDED.name
   OR country.active IS DISTINCT FROM EXCLUDED.active;

COMMIT;
