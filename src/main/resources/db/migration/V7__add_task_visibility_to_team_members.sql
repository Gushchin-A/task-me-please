ALTER TABLE team_members
ADD COLUMN task_visibility VARCHAR(30) NOT NULL DEFAULT 'OWN_TASKS';

UPDATE team_members
SET task_visibility = 'ALL_TASKS'
WHERE role = 'OWNER';