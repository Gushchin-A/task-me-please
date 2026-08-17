ALTER TABLE tasks
ADD COLUMN archived_by UUID;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_archived_by
FOREIGN KEY (archived_by) REFERENCES users (id);
