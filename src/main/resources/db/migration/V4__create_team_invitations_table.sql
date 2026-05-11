CREATE TABLE team_invitations (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    invited_by UUID NOT NULL,
    invited_email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_team_invitations_team_id
        FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_team_invitations_invited_by
        FOREIGN KEY (invited_by) REFERENCES users (id)
);