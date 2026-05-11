CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    author_id UUID NOT NULL,
    assignee_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tasks_team_id
        FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_tasks_author_id
        FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_assignee_id
        FOREIGN KEY (assignee_id) REFERENCES users (id)
);