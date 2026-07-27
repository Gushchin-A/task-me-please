CREATE TABLE account_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_account_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_account_tokens_type
        CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_account_tokens_user_id_type
    ON account_tokens (user_id, type);
