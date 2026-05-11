CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comments_task_id
        FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_comments_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);