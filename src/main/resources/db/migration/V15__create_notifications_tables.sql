CREATE TABLE notification_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    actor_user_id UUID,
    team_id BIGINT,
    task_id BIGINT,
    comment_id BIGINT,
    invitation_id BIGINT,
    subject_user_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_notification_events_actor_user_id
        FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT fk_notification_events_team_id
        FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_notification_events_task_id
        FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_notification_events_comment_id
        FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT fk_notification_events_invitation_id
        FOREIGN KEY (invitation_id) REFERENCES team_invitations (id),
    CONSTRAINT fk_notification_events_subject_user_id
        FOREIGN KEY (subject_user_id) REFERENCES users (id)
);

CREATE TABLE user_notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_event_id BIGINT NOT NULL,
    recipient_user_id UUID,
    recipient_email VARCHAR(255),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_notifications_notification_event_id
        FOREIGN KEY (notification_event_id) REFERENCES notification_events (id),
    CONSTRAINT fk_user_notifications_recipient_user_id
        FOREIGN KEY (recipient_user_id) REFERENCES users (id),
    CONSTRAINT ck_user_notifications_single_recipient
        CHECK ((recipient_user_id IS NOT NULL) <> (recipient_email IS NOT NULL)),
    CONSTRAINT uk_user_notifications_event_user
        UNIQUE (notification_event_id, recipient_user_id)
);

CREATE UNIQUE INDEX uk_user_notifications_event_email
    ON user_notifications (notification_event_id, recipient_email)
    WHERE recipient_email IS NOT NULL;

CREATE INDEX idx_user_notifications_user_id_desc
    ON user_notifications (recipient_user_id, id DESC)
    WHERE recipient_user_id IS NOT NULL;

CREATE INDEX idx_user_notifications_user_unread
    ON user_notifications (recipient_user_id, id DESC)
    WHERE recipient_user_id IS NOT NULL AND read_at IS NULL;

CREATE INDEX idx_user_notifications_recipient_email
    ON user_notifications (recipient_email)
    WHERE recipient_email IS NOT NULL;
