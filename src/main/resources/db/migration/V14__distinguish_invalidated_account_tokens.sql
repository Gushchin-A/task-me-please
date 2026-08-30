ALTER TABLE account_tokens
    ADD COLUMN invalidated_at TIMESTAMP WITH TIME ZONE;

UPDATE account_tokens AS previous_token
SET invalidated_at = previous_token.used_at,
    used_at = NULL
WHERE previous_token.type = 'EMAIL_VERIFICATION'
  AND previous_token.used_at IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM account_tokens AS newer_token
      WHERE newer_token.user_id = previous_token.user_id
        AND newer_token.type = previous_token.type
        AND newer_token.created_at > previous_token.created_at
  );
