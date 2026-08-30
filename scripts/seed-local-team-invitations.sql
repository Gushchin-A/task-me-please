BEGIN;

WITH invitation_seed(invited_email, token, status, expires_at, created_at, updated_at) AS (
    VALUES
        (
            'nedra.chubakka@mail.ru',
            'local-team-14-chubakka-declined-first-invite',
            'DECLINED',
            CURRENT_TIMESTAMP - INTERVAL '17 days',
            CURRENT_TIMESTAMP - INTERVAL '24 days',
            CURRENT_TIMESTAMP - INTERVAL '22 days'
        ),
        (
            'nedra.chubakka@mail.ru',
            'local-team-14-chubakka-accepted-second-invite',
            'ACCEPTED',
            CURRENT_TIMESTAMP - INTERVAL '9 days',
            CURRENT_TIMESTAMP - INTERVAL '16 days',
            CURRENT_TIMESTAMP - INTERVAL '15 days'
        ),
        (
            'nedra.shtirlitz@mail.ru',
            'local-team-14-shtirlitz-accepted-invite',
            'ACCEPTED',
            CURRENT_TIMESTAMP - INTERVAL '5 days',
            CURRENT_TIMESTAMP - INTERVAL '12 days',
            CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '18 minutes'
        ),
        (
            'nedra.yoda@mail.ru',
            'local-team-14-yoda-accepted-invite-history',
            'ACCEPTED',
            CURRENT_TIMESTAMP - INTERVAL '2 days',
            CURRENT_TIMESTAMP - INTERVAL '9 days',
            CURRENT_TIMESTAMP - INTERVAL '8 days'
        ),
        (
            'ilya.sokolov.with.long.email@example.com',
            'local-team-14-ilya-accepted-invite-history',
            'ACCEPTED',
            CURRENT_TIMESTAMP + INTERVAL '1 day',
            CURRENT_TIMESTAMP - INTERVAL '6 days',
            CURRENT_TIMESTAMP - INTERVAL '5 days'
        ),
        (
            'anna.petrova@example.com',
            'local-team-14-anna-pending-active-invitation',
            'PENDING',
            CURRENT_TIMESTAMP + INTERVAL '3 days',
            CURRENT_TIMESTAMP - INTERVAL '4 days',
            CURRENT_TIMESTAMP - INTERVAL '4 days'
        ),
        (
            'maxim.volkov@example.com',
            'local-team-14-maxim-pending-active-invitation',
            'PENDING',
            CURRENT_TIMESTAMP + INTERVAL '5 days',
            CURRENT_TIMESTAMP - INTERVAL '2 days',
            CURRENT_TIMESTAMP - INTERVAL '2 days'
        ),
        (
            'very.long.invitation.email.address@example-company.com',
            'local-team-14-long-email-pending-invitation',
            'PENDING',
            CURRENT_TIMESTAMP + INTERVAL '7 days',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        ),
        (
            'olga.expired@example.com',
            'local-team-14-olga-expired-invitation-link',
            'EXPIRED',
            CURRENT_TIMESTAMP - INTERVAL '3 days',
            CURRENT_TIMESTAMP - INTERVAL '10 days',
            CURRENT_TIMESTAMP - INTERVAL '3 days'
        )
)
INSERT INTO team_invitations (
    team_id,
    invited_by,
    invited_email,
    token,
    status,
    expires_at,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    team_members.team_id,
    team_members.user_id,
    invitation_seed.invited_email,
    invitation_seed.token,
    invitation_seed.status,
    invitation_seed.expires_at,
    invitation_seed.created_at,
    invitation_seed.updated_at,
    FALSE
FROM invitation_seed
JOIN team_members
    ON team_members.team_id = 14
    AND team_members.role = 'OWNER'
    AND team_members.is_deleted = FALSE
ON CONFLICT (token) DO UPDATE SET
    team_id = EXCLUDED.team_id,
    invited_by = EXCLUDED.invited_by,
    invited_email = EXCLUDED.invited_email,
    status = EXCLUDED.status,
    expires_at = EXCLUDED.expires_at,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at,
    is_deleted = FALSE;

COMMIT;
