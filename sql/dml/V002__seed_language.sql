BEGIN;

INSERT INTO language (code, name)
VALUES ('en', 'English'),
       ('bn', 'Bangla')
ON CONFLICT (code)
  DO UPDATE
  SET name = EXCLUDED.name
WHERE language.name IS DISTINCT FROM EXCLUDED.name;

COMMIT;
