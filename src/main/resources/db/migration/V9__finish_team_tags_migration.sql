ALTER TABLE tasks
ALTER COLUMN tag_id SET NOT NULL;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_tag_id
    FOREIGN KEY (tag_id) REFERENCES team_tags (id);

ALTER TABLE tasks
DROP COLUMN category;