UPDATE tasks
SET deadline_at = created_at
WHERE deadline_at IS NULL;

ALTER TABLE tasks
    ALTER COLUMN deadline_at SET NOT NULL;
