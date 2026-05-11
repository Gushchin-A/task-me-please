CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_team_members_team_id
        FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_team_members_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_team_members_team_id_user_id
        UNIQUE (team_id, user_id)
);