BEGIN;

INSERT INTO teams (name, created_by, created_at, updated_at, is_deleted)
SELECT
    'Запуск мобильного приложения',
    users.id,
    CURRENT_TIMESTAMP - INTERVAL '18 days',
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    FALSE
FROM users
WHERE lower(users.email) = 'a.s.gushchin@yandex.ru'
  AND NOT EXISTS (
      SELECT 1
      FROM teams
      WHERE teams.name = 'Запуск мобильного приложения'
        AND teams.created_by = users.id
  );

INSERT INTO teams (name, created_by, created_at, updated_at, is_deleted)
SELECT
    'Редакция продуктового блога',
    users.id,
    CURRENT_TIMESTAMP - INTERVAL '9 days',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    FALSE
FROM users
WHERE lower(users.email) = 'a.s.gushchin@yandex.ru'
  AND NOT EXISTS (
      SELECT 1
      FROM teams
      WHERE teams.name = 'Редакция продуктового блога'
        AND teams.created_by = users.id
  );

WITH seed_members(team_name, email, role, task_visibility, joined_ago) AS (
    VALUES
        ('Запуск мобильного приложения', 'a.s.gushchin@yandex.ru', 'OWNER', 'ALL_TASKS', INTERVAL '18 days'),
        ('Запуск мобильного приложения', 'sosana-sasana@yandex.com', 'MEMBER', 'ALL_TASKS', INTERVAL '14 days'),
        ('Запуск мобильного приложения', 'nedra.yoda@mail.ru', 'MEMBER', 'OWN_TASKS', INTERVAL '12 days'),
        ('Запуск мобильного приложения', 'nedra.shtirlitz@mail.ru', 'MEMBER', 'OWN_TASKS', INTERVAL '7 days'),
        ('Редакция продуктового блога', 'a.s.gushchin@yandex.ru', 'OWNER', 'ALL_TASKS', INTERVAL '9 days')
)
INSERT INTO team_members (
    team_id,
    user_id,
    role,
    joined_at,
    created_at,
    updated_at,
    is_deleted,
    task_visibility
)
SELECT
    teams.id,
    users.id,
    seed_members.role,
    CURRENT_TIMESTAMP - seed_members.joined_ago,
    CURRENT_TIMESTAMP - seed_members.joined_ago,
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    FALSE,
    seed_members.task_visibility
FROM seed_members
JOIN users ON lower(users.email) = seed_members.email
JOIN teams ON teams.name = seed_members.team_name
JOIN users owners ON owners.id = teams.created_by AND lower(owners.email) = 'a.s.gushchin@yandex.ru'
ON CONFLICT (team_id, user_id) DO UPDATE SET
    role = EXCLUDED.role,
    task_visibility = EXCLUDED.task_visibility,
    is_deleted = FALSE,
    updated_at = EXCLUDED.updated_at;

WITH seed_tags(team_name, name, normalized_name, created_ago) AS (
    VALUES
        ('Запуск мобильного приложения', 'Разработка', 'разработка', INTERVAL '17 days'),
        ('Запуск мобильного приложения', 'Дизайн', 'дизайн', INTERVAL '16 days'),
        ('Запуск мобильного приложения', 'Исследования', 'исследования', INTERVAL '15 days'),
        ('Запуск мобильного приложения', 'Релиз', 'релиз', INTERVAL '8 days'),
        ('Редакция продуктового блога', 'Статья', 'статья', INTERVAL '8 days'),
        ('Редакция продуктового блога', 'Редактура', 'редактура', INTERVAL '7 days')
)
INSERT INTO team_tags (team_id, name, normalized_name, created_at, updated_at, is_deleted)
SELECT
    teams.id,
    seed_tags.name,
    seed_tags.normalized_name,
    CURRENT_TIMESTAMP - seed_tags.created_ago,
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    FALSE
FROM seed_tags
JOIN teams ON teams.name = seed_tags.team_name
JOIN users owners ON owners.id = teams.created_by AND lower(owners.email) = 'a.s.gushchin@yandex.ru'
WHERE NOT EXISTS (
    SELECT 1
    FROM team_tags
    WHERE team_tags.team_id = teams.id
      AND team_tags.normalized_name = seed_tags.normalized_name
      AND team_tags.is_deleted = FALSE
);

WITH seed_tasks(
    team_name,
    author_email,
    assignee_email,
    title,
    description,
    deadline_ago,
    status,
    is_archived,
    archived_by_email,
    tag_name,
    created_ago,
    updated_ago
) AS (
    VALUES
        ('Запуск мобильного приложения', 'a.s.gushchin@yandex.ru', 'sosana-sasana@yandex.com',
         'Подготовить сценарии первого запуска', 'Собрать тексты, состояния и проверить путь нового пользователя.',
         INTERVAL '-4 days', 'IN_PROGRESS', FALSE, NULL, 'Исследования', INTERVAL '13 days', INTERVAL '45 minutes'),
        ('Запуск мобильного приложения', 'sosana-sasana@yandex.com', 'a.s.gushchin@yandex.ru',
         'Проверить аналитику экрана задач', 'Сверить события аналитики и итоговую воронку.',
         INTERVAL '-2 days', 'OPEN', FALSE, NULL, 'Разработка', INTERVAL '11 days', INTERVAL '3 hours'),
        ('Запуск мобильного приложения', 'a.s.gushchin@yandex.ru', 'a.s.gushchin@yandex.ru',
         'Согласовать макеты карточки задачи', 'Проверить desktop и mobile состояния карточки.',
         INTERVAL '1 day', 'DONE', FALSE, NULL, 'Дизайн', INTERVAL '10 days', INTERVAL '1 day'),
        ('Запуск мобильного приложения', 'sosana-sasana@yandex.com', 'sosana-sasana@yandex.com',
         'Обновить чек-лист публикации', 'Зафиксировать шаги перед выкладкой новой версии.',
         INTERVAL '3 days', 'DONE', TRUE, 'sosana-sasana@yandex.com', 'Релиз', INTERVAL '8 days', INTERVAL '6 hours'),
        ('Запуск мобильного приложения', 'a.s.gushchin@yandex.ru', 'nedra.yoda@mail.ru',
         'Провести регрессионную проверку', 'Пройти критические пользовательские сценарии.',
         INTERVAL '-1 day', 'IN_PROGRESS', FALSE, NULL, 'Релиз', INTERVAL '6 days', INTERVAL '2 hours'),
        ('Запуск мобильного приложения', 'sosana-sasana@yandex.com', 'a.s.gushchin@yandex.ru',
         'Описать известные ограничения релиза', 'Подготовить короткий список ограничений для команды поддержки.',
         INTERVAL '-5 days', 'NOT_RELEVANT', TRUE, 'a.s.gushchin@yandex.ru', 'Релиз', INTERVAL '5 days', INTERVAL '2 days'),
        ('Редакция продуктового блога', 'a.s.gushchin@yandex.ru', 'a.s.gushchin@yandex.ru',
         'Написать заметку о новом планировщике', 'Подготовить черновик и примеры рабочих сценариев.',
         INTERVAL '-6 days', 'IN_PROGRESS', FALSE, NULL, 'Статья', INTERVAL '7 days', INTERVAL '25 minutes'),
        ('Редакция продуктового блога', 'a.s.gushchin@yandex.ru', 'a.s.gushchin@yandex.ru',
         'Вычитать анонс обновления', 'Убрать повторы и проверить ссылки.',
         INTERVAL '2 days', 'DONE', TRUE, 'a.s.gushchin@yandex.ru', 'Редактура', INTERVAL '4 days', INTERVAL '1 day')
)
INSERT INTO tasks (
    team_id,
    author_id,
    assignee_id,
    title,
    description,
    deadline_at,
    status,
    is_archived,
    is_deleted,
    created_at,
    updated_at,
    tag_id,
    archived_by
)
SELECT
    teams.id,
    authors.id,
    assignees.id,
    seed_tasks.title,
    seed_tasks.description,
    CURRENT_TIMESTAMP - seed_tasks.deadline_ago,
    seed_tasks.status,
    seed_tasks.is_archived,
    FALSE,
    CURRENT_TIMESTAMP - seed_tasks.created_ago,
    CURRENT_TIMESTAMP - seed_tasks.updated_ago,
    team_tags.id,
    archivers.id
FROM seed_tasks
JOIN teams ON teams.name = seed_tasks.team_name
JOIN users owners ON owners.id = teams.created_by AND lower(owners.email) = 'a.s.gushchin@yandex.ru'
JOIN users authors ON lower(authors.email) = seed_tasks.author_email
JOIN users assignees ON lower(assignees.email) = seed_tasks.assignee_email
JOIN team_tags ON team_tags.team_id = teams.id AND team_tags.name = seed_tasks.tag_name
LEFT JOIN users archivers ON lower(archivers.email) = seed_tasks.archived_by_email
WHERE NOT EXISTS (
    SELECT 1
    FROM tasks
    WHERE tasks.team_id = teams.id
      AND tasks.title = seed_tasks.title
);

WITH seed_comments(task_title, author_email, message, created_ago, is_deleted) AS (
    VALUES
        ('Подготовить сценарии первого запуска', 'sosana-sasana@yandex.com',
         'Добавил сценарий возврата пользователя и состояние без данных.', INTERVAL '2 days', FALSE),
        ('Подготовить сценарии первого запуска', 'a.s.gushchin@yandex.ru',
         'Проверил тексты. Осталось пройти мобильную версию.', INTERVAL '20 hours', FALSE),
        ('Проверить аналитику экрана задач', 'sosana-sasana@yandex.com',
         'Исправил названия двух событий и обновил таблицу параметров.', INTERVAL '5 hours', FALSE),
        ('Согласовать макеты карточки задачи', 'a.s.gushchin@yandex.ru',
         'Финальный вариант согласован, можно передавать в разработку.', INTERVAL '1 day', FALSE),
        ('Обновить чек-лист публикации', 'sosana-sasana@yandex.com',
         'Старый комментарий с неактуальным порядком шагов.', INTERVAL '4 days', TRUE),
        ('Написать заметку о новом планировщике', 'a.s.gushchin@yandex.ru',
         'Добавил живой пример работы небольшой команды.', INTERVAL '50 minutes', FALSE)
)
INSERT INTO comments (task_id, user_id, message, is_deleted, created_at, updated_at)
SELECT
    tasks.id,
    users.id,
    seed_comments.message,
    seed_comments.is_deleted,
    CURRENT_TIMESTAMP - seed_comments.created_ago,
    CURRENT_TIMESTAMP - seed_comments.created_ago + INTERVAL '15 minutes'
FROM seed_comments
JOIN tasks ON tasks.title = seed_comments.task_title
JOIN teams ON teams.id = tasks.team_id
JOIN users owners ON owners.id = teams.created_by AND lower(owners.email) = 'a.s.gushchin@yandex.ru'
JOIN users ON lower(users.email) = seed_comments.author_email
WHERE NOT EXISTS (
    SELECT 1
    FROM comments
    WHERE comments.task_id = tasks.id
      AND comments.message = seed_comments.message
);

WITH seed_invitations(invited_email, token, status, expires_ago, created_ago, updated_ago) AS (
    VALUES
        ('nedra.yoda@mail.ru', 'local-notifications-mobile-yoda-accepted', 'ACCEPTED',
         INTERVAL '5 days', INTERVAL '13 days', INTERVAL '12 days'),
        ('nedra.chubakka@mail.ru', 'local-notifications-mobile-chubakka-declined', 'DECLINED',
         INTERVAL '3 days', INTERVAL '11 days', INTERVAL '10 days'),
        ('nedra.shtirlitz@mail.ru', 'local-notifications-mobile-shtirlitz-accepted', 'ACCEPTED',
         INTERVAL '1 day', INTERVAL '8 days', INTERVAL '7 days'),
        ('newUser5@mail.ru', 'local-notifications-mobile-credo-expired', 'EXPIRED',
         INTERVAL '2 days', INTERVAL '9 days', INTERVAL '2 days'),
        ('sosana-sasana@yandex.com', 'local-notifications-editorial-sosana-active', 'PENDING',
         INTERVAL '-7 days', INTERVAL '35 minutes', INTERVAL '35 minutes')
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
    CASE
        WHEN seed_invitations.invited_email = 'sosana-sasana@yandex.com' THEN editorial_team.id
        ELSE mobile_team.id
    END,
    owners.id,
    seed_invitations.invited_email,
    seed_invitations.token,
    seed_invitations.status,
    CURRENT_TIMESTAMP - seed_invitations.expires_ago,
    CURRENT_TIMESTAMP - seed_invitations.created_ago,
    CURRENT_TIMESTAMP - seed_invitations.updated_ago,
    FALSE
FROM seed_invitations
JOIN users owners ON lower(owners.email) = 'a.s.gushchin@yandex.ru'
JOIN teams mobile_team ON mobile_team.name = 'Запуск мобильного приложения' AND mobile_team.created_by = owners.id
JOIN teams editorial_team ON editorial_team.name = 'Редакция продуктового блога' AND editorial_team.created_by = owners.id
ON CONFLICT (token) DO UPDATE SET
    team_id = EXCLUDED.team_id,
    invited_by = EXCLUDED.invited_by,
    invited_email = EXCLUDED.invited_email,
    status = EXCLUDED.status,
    expires_at = EXCLUDED.expires_at,
    updated_at = EXCLUDED.updated_at,
    is_deleted = FALSE;

CREATE TEMPORARY TABLE seed_notification_events (
    seed_key VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    actor_user_id UUID,
    team_id BIGINT,
    task_id BIGINT,
    comment_id BIGINT,
    invitation_id BIGINT,
    subject_user_id UUID,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
) ON COMMIT DROP;

WITH context AS (
    SELECT
        owner_user.id AS owner_id,
        owner_user.name AS owner_name,
        member_user.id AS member_id,
        member_user.name AS member_name,
        yoda_user.id AS yoda_id,
        yoda_user.name AS yoda_name,
        shtirlitz_user.id AS shtirlitz_id,
        shtirlitz_user.name AS shtirlitz_name,
        chubakka_user.id AS chubakka_id,
        chubakka_user.name AS chubakka_name,
        mobile_team.id AS mobile_team_id,
        mobile_team.name AS mobile_team_name,
        editorial_team.id AS editorial_team_id,
        editorial_team.name AS editorial_team_name
    FROM users owner_user
    JOIN users member_user ON lower(member_user.email) = 'sosana-sasana@yandex.com'
    JOIN users yoda_user ON lower(yoda_user.email) = 'nedra.yoda@mail.ru'
    JOIN users shtirlitz_user ON lower(shtirlitz_user.email) = 'nedra.shtirlitz@mail.ru'
    JOIN users chubakka_user ON lower(chubakka_user.email) = 'nedra.chubakka@mail.ru'
    JOIN teams mobile_team ON mobile_team.name = 'Запуск мобильного приложения'
        AND mobile_team.created_by = owner_user.id
    JOIN teams editorial_team ON editorial_team.name = 'Редакция продуктового блога'
        AND editorial_team.created_by = owner_user.id
    WHERE lower(owner_user.email) = 'a.s.gushchin@yandex.ru'
), task_context AS (
    SELECT
        context.*,
        onboarding.id AS onboarding_id,
        analytics.id AS analytics_id,
        layouts.id AS layouts_id,
        checklist.id AS checklist_id,
        regression.id AS regression_id,
        limits_task.id AS limits_id,
        article.id AS article_id,
        announcement.id AS announcement_id
    FROM context
    JOIN tasks onboarding ON onboarding.team_id = context.mobile_team_id
        AND onboarding.title = 'Подготовить сценарии первого запуска'
    JOIN tasks analytics ON analytics.team_id = context.mobile_team_id
        AND analytics.title = 'Проверить аналитику экрана задач'
    JOIN tasks layouts ON layouts.team_id = context.mobile_team_id
        AND layouts.title = 'Согласовать макеты карточки задачи'
    JOIN tasks checklist ON checklist.team_id = context.mobile_team_id
        AND checklist.title = 'Обновить чек-лист публикации'
    JOIN tasks regression ON regression.team_id = context.mobile_team_id
        AND regression.title = 'Провести регрессионную проверку'
    JOIN tasks limits_task ON limits_task.team_id = context.mobile_team_id
        AND limits_task.title = 'Описать известные ограничения релиза'
    JOIN tasks article ON article.team_id = context.editorial_team_id
        AND article.title = 'Написать заметку о новом планировщике'
    JOIN tasks announcement ON announcement.team_id = context.editorial_team_id
        AND announcement.title = 'Вычитать анонс обновления'
), invitation_context AS (
    SELECT
        task_context.*,
        yoda_invitation.id AS yoda_invitation_id,
        chubakka_invitation.id AS chubakka_invitation_id,
        shtirlitz_invitation.id AS shtirlitz_invitation_id,
        expired_invitation.id AS expired_invitation_id,
        member_invitation.id AS member_invitation_id
    FROM task_context
    JOIN team_invitations yoda_invitation ON yoda_invitation.token = 'local-notifications-mobile-yoda-accepted'
    JOIN team_invitations chubakka_invitation ON chubakka_invitation.token = 'local-notifications-mobile-chubakka-declined'
    JOIN team_invitations shtirlitz_invitation ON shtirlitz_invitation.token = 'local-notifications-mobile-shtirlitz-accepted'
    JOIN team_invitations expired_invitation ON expired_invitation.token = 'local-notifications-mobile-credo-expired'
    JOIN team_invitations member_invitation ON member_invitation.token = 'local-notifications-editorial-sosana-active'
)
INSERT INTO seed_notification_events (
    seed_key,
    event_type,
    actor_user_id,
    team_id,
    task_id,
    comment_id,
    invitation_id,
    subject_user_id,
    payload,
    created_at
)
SELECT
    seed_rows.seed_key,
    seed_rows.event_type,
    seed_rows.actor_user_id,
    seed_rows.team_id,
    seed_rows.task_id,
    seed_rows.comment_id,
    seed_rows.invitation_id,
    seed_rows.subject_user_id,
    seed_rows.payload,
    CURRENT_TIMESTAMP - seed_rows.created_ago
FROM invitation_context context
CROSS JOIN LATERAL (
    VALUES
        ('team-created-mobile', 'TEAM_CREATED', context.owner_id, context.mobile_team_id, NULL::BIGINT, NULL::BIGINT,
         NULL::BIGINT, NULL::UUID,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'subjectName', NULL, 'previousValue', NULL, 'newValue', NULL), INTERVAL '18 days'),
        ('invitation-yoda-accepted', 'TEAM_INVITATION_ACCEPTED', context.yoda_id, context.mobile_team_id, NULL, NULL,
         context.yoda_invitation_id, NULL,
         jsonb_build_object('actorName', context.yoda_name, 'teamName', context.mobile_team_name,
             'invitedEmail', 'nedra.yoda@mail.ru'), INTERVAL '12 days'),
        ('member-yoda-joined', 'TEAM_MEMBER_JOINED', context.yoda_id, context.mobile_team_id, NULL, NULL, NULL,
         context.yoda_id,
         jsonb_build_object('actorName', context.yoda_name, 'teamName', context.mobile_team_name,
             'subjectName', context.yoda_name, 'previousValue', NULL, 'newValue', NULL), INTERVAL '12 days'),
        ('invitation-chubakka-declined', 'TEAM_INVITATION_DECLINED', context.chubakka_id, context.mobile_team_id,
         NULL, NULL, context.chubakka_invitation_id, NULL,
         jsonb_build_object('actorName', context.chubakka_name, 'teamName', context.mobile_team_name,
             'invitedEmail', 'nedra.chubakka@mail.ru'), INTERVAL '10 days'),
        ('invitation-shtirlitz-accepted', 'TEAM_INVITATION_ACCEPTED', context.shtirlitz_id, context.mobile_team_id,
         NULL, NULL, context.shtirlitz_invitation_id, NULL,
         jsonb_build_object('actorName', context.shtirlitz_name, 'teamName', context.mobile_team_name,
             'invitedEmail', 'nedra.shtirlitz@mail.ru'), INTERVAL '7 days'),
        ('invitation-expired', 'TEAM_INVITATION_EXPIRED', context.owner_id, context.mobile_team_id, NULL, NULL,
         context.expired_invitation_id, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'invitedEmail', 'newUser5@mail.ru'), INTERVAL '2 days'),
        ('team-created-editorial', 'TEAM_CREATED', context.owner_id, context.editorial_team_id, NULL, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.editorial_team_name,
             'subjectName', NULL, 'previousValue', NULL, 'newValue', NULL), INTERVAL '9 days'),
        ('invitation-member-active', 'TEAM_INVITATION_CREATED', context.owner_id, context.editorial_team_id,
         NULL, NULL, context.member_invitation_id, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.editorial_team_name,
             'invitedEmail', 'sosana-sasana@yandex.com'), INTERVAL '35 minutes'),
        ('task-onboarding-created', 'TASK_CREATED', context.owner_id, context.mobile_team_id, context.onboarding_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска', 'previousValue', NULL, 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.member_id)), INTERVAL '13 days'),
        ('task-analytics-created', 'TASK_CREATED', context.member_id, context.mobile_team_id, context.analytics_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Проверить аналитику экрана задач', 'previousValue', NULL, 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.member_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '11 days'),
        ('task-layouts-created', 'TASK_CREATED', context.owner_id, context.mobile_team_id, context.layouts_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Согласовать макеты карточки задачи', 'previousValue', NULL, 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '10 days'),
        ('task-onboarding-status', 'TASK_STATUS_CHANGED', context.member_id, context.mobile_team_id,
         context.onboarding_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска', 'previousValue', 'OPEN',
             'newValue', 'IN_PROGRESS', 'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.member_id)), INTERVAL '2 days 4 hours'),
        ('task-onboarding-deadline', 'TASK_DEADLINE_CHANGED', context.owner_id, context.mobile_team_id,
         context.onboarding_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска', 'previousValue', '2026-09-02',
             'newValue', '2026-09-04', 'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.member_id)), INTERVAL '1 day 18 hours'),
        ('task-onboarding-description', 'TASK_DESCRIPTION_CHANGED', context.member_id, context.mobile_team_id,
         context.onboarding_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска', 'previousValue', NULL, 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.member_id)), INTERVAL '1 day 4 hours'),
        ('task-analytics-assignee', 'TASK_ASSIGNEE_CHANGED', context.member_id, context.mobile_team_id,
         context.analytics_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Проверить аналитику экрана задач', 'previousValue', context.member_name,
             'newValue', context.owner_name, 'previousUserId', context.member_id, 'newUserId', context.owner_id,
             'participants', jsonb_build_object('authorUserId', context.member_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '3 hours'),
        ('task-layouts-title', 'TASK_TITLE_CHANGED', context.owner_id, context.mobile_team_id, context.layouts_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Согласовать макеты карточки задачи', 'previousValue', 'Проверить макеты карточки',
             'newValue', 'Согласовать макеты карточки задачи', 'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '3 days'),
        ('task-checklist-archived', 'TASK_ARCHIVED', context.member_id, context.mobile_team_id,
         context.checklist_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Обновить чек-лист публикации', 'previousValue', 'DONE', 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.member_id,
                 'assigneeUserId', context.member_id)), INTERVAL '6 hours'),
        ('task-regression-tag', 'TASK_TAG_CHANGED', context.owner_id, context.mobile_team_id,
         context.regression_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Провести регрессионную проверку', 'previousValue', 'Разработка',
             'newValue', 'Релиз', 'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.yoda_id)), INTERVAL '2 hours'),
        ('task-limits-archived', 'TASK_ARCHIVED', context.owner_id, context.mobile_team_id, context.limits_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Описать известные ограничения релиза', 'previousValue', 'NOT_RELEVANT', 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.member_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '2 days'),
        ('task-article-created', 'TASK_CREATED', context.owner_id, context.editorial_team_id, context.article_id,
         NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.editorial_team_name,
             'taskTitle', 'Написать заметку о новом планировщике', 'previousValue', NULL, 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '7 days'),
        ('task-article-status', 'TASK_STATUS_CHANGED', context.owner_id, context.editorial_team_id,
         context.article_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.editorial_team_name,
             'taskTitle', 'Написать заметку о новом планировщике', 'previousValue', 'OPEN',
             'newValue', 'IN_PROGRESS', 'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '25 minutes'),
        ('task-announcement-archived', 'TASK_ARCHIVED', context.owner_id, context.editorial_team_id,
         context.announcement_id, NULL, NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.editorial_team_name,
             'taskTitle', 'Вычитать анонс обновления', 'previousValue', 'DONE', 'newValue', NULL,
             'previousUserId', NULL, 'newUserId', NULL,
             'participants', jsonb_build_object('authorUserId', context.owner_id,
                 'assigneeUserId', context.owner_id)), INTERVAL '1 day'),
        ('comment-onboarding-member', 'COMMENT_CREATED', context.member_id, context.mobile_team_id,
         context.onboarding_id,
         (SELECT id FROM comments WHERE task_id = context.onboarding_id
              AND message = 'Добавил сценарий возврата пользователя и состояние без данных.'), NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска'), INTERVAL '2 days'),
        ('comment-onboarding-owner', 'COMMENT_CREATED', context.owner_id, context.mobile_team_id,
         context.onboarding_id,
         (SELECT id FROM comments WHERE task_id = context.onboarding_id
              AND message = 'Проверил тексты. Осталось пройти мобильную версию.'), NULL, NULL,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Подготовить сценарии первого запуска'), INTERVAL '20 hours'),
        ('comment-analytics-updated', 'COMMENT_UPDATED', context.member_id, context.mobile_team_id,
         context.analytics_id,
         (SELECT id FROM comments WHERE task_id = context.analytics_id
              AND message = 'Исправил названия двух событий и обновил таблицу параметров.'), NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Проверить аналитику экрана задач'), INTERVAL '4 hours 45 minutes'),
        ('comment-checklist-deleted', 'COMMENT_DELETED', context.member_id, context.mobile_team_id,
         context.checklist_id,
         (SELECT id FROM comments WHERE task_id = context.checklist_id
              AND message = 'Старый комментарий с неактуальным порядком шагов.'), NULL, NULL,
         jsonb_build_object('actorName', context.member_name, 'teamName', context.mobile_team_name,
             'taskTitle', 'Обновить чек-лист публикации'), INTERVAL '3 days 23 hours'),
        ('visibility-member-granted', 'TEAM_TASK_VISIBILITY_GRANTED', context.owner_id, context.mobile_team_id,
         NULL, NULL, NULL, context.member_id,
         jsonb_build_object('actorName', context.owner_name, 'teamName', context.mobile_team_name,
             'subjectName', context.member_name, 'previousValue', 'OWN_TASKS', 'newValue', 'ALL_TASKS'),
         INTERVAL '1 day 2 hours')
) AS seed_rows(
    seed_key,
    event_type,
    actor_user_id,
    team_id,
    task_id,
    comment_id,
    invitation_id,
    subject_user_id,
    payload,
    created_ago
);

INSERT INTO notification_events (
    event_type,
    actor_user_id,
    team_id,
    task_id,
    comment_id,
    invitation_id,
    subject_user_id,
    payload,
    created_at
)
SELECT
    seed.event_type,
    seed.actor_user_id,
    seed.team_id,
    seed.task_id,
    seed.comment_id,
    seed.invitation_id,
    seed.subject_user_id,
    seed.payload,
    seed.created_at
FROM seed_notification_events seed
WHERE NOT EXISTS (
    SELECT 1
    FROM notification_events events
    WHERE events.event_type = seed.event_type
      AND events.actor_user_id IS NOT DISTINCT FROM seed.actor_user_id
      AND events.team_id IS NOT DISTINCT FROM seed.team_id
      AND events.task_id IS NOT DISTINCT FROM seed.task_id
      AND events.comment_id IS NOT DISTINCT FROM seed.comment_id
      AND events.invitation_id IS NOT DISTINCT FROM seed.invitation_id
      AND events.subject_user_id IS NOT DISTINCT FROM seed.subject_user_id
      AND events.payload = seed.payload
);

CREATE TEMPORARY TABLE seed_notification_recipients (
    seed_key VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    read_ago INTERVAL
) ON COMMIT DROP;

INSERT INTO seed_notification_recipients (seed_key, recipient_email, read_ago)
VALUES
    ('team-created-mobile', 'a.s.gushchin@yandex.ru', INTERVAL '16 days'),
    ('invitation-yoda-accepted', 'a.s.gushchin@yandex.ru', INTERVAL '11 days'),
    ('member-yoda-joined', 'a.s.gushchin@yandex.ru', INTERVAL '11 days'),
    ('invitation-chubakka-declined', 'a.s.gushchin@yandex.ru', INTERVAL '9 days'),
    ('invitation-shtirlitz-accepted', 'a.s.gushchin@yandex.ru', INTERVAL '6 days'),
    ('invitation-expired', 'a.s.gushchin@yandex.ru', NULL),
    ('team-created-editorial', 'a.s.gushchin@yandex.ru', INTERVAL '8 days'),
    ('invitation-member-active', 'a.s.gushchin@yandex.ru', NULL),
    ('invitation-member-active', 'sosana-sasana@yandex.com', NULL),
    ('task-onboarding-created', 'a.s.gushchin@yandex.ru', INTERVAL '12 days'),
    ('task-onboarding-created', 'sosana-sasana@yandex.com', INTERVAL '12 days'),
    ('task-analytics-created', 'a.s.gushchin@yandex.ru', INTERVAL '10 days'),
    ('task-analytics-created', 'sosana-sasana@yandex.com', INTERVAL '10 days'),
    ('task-layouts-created', 'a.s.gushchin@yandex.ru', INTERVAL '9 days'),
    ('task-onboarding-status', 'a.s.gushchin@yandex.ru', NULL),
    ('task-onboarding-status', 'sosana-sasana@yandex.com', NULL),
    ('task-onboarding-deadline', 'a.s.gushchin@yandex.ru', INTERVAL '1 day'),
    ('task-onboarding-deadline', 'sosana-sasana@yandex.com', NULL),
    ('task-onboarding-description', 'a.s.gushchin@yandex.ru', NULL),
    ('task-onboarding-description', 'sosana-sasana@yandex.com', NULL),
    ('task-analytics-assignee', 'a.s.gushchin@yandex.ru', NULL),
    ('task-analytics-assignee', 'sosana-sasana@yandex.com', NULL),
    ('task-layouts-title', 'a.s.gushchin@yandex.ru', INTERVAL '2 days'),
    ('task-checklist-archived', 'sosana-sasana@yandex.com', NULL),
    ('task-regression-tag', 'a.s.gushchin@yandex.ru', NULL),
    ('task-limits-archived', 'a.s.gushchin@yandex.ru', NULL),
    ('task-limits-archived', 'sosana-sasana@yandex.com', NULL),
    ('task-article-created', 'a.s.gushchin@yandex.ru', INTERVAL '6 days'),
    ('task-article-status', 'a.s.gushchin@yandex.ru', NULL),
    ('task-announcement-archived', 'a.s.gushchin@yandex.ru', NULL),
    ('comment-onboarding-member', 'a.s.gushchin@yandex.ru', NULL),
    ('comment-onboarding-member', 'sosana-sasana@yandex.com', INTERVAL '1 day'),
    ('comment-onboarding-owner', 'a.s.gushchin@yandex.ru', NULL),
    ('comment-onboarding-owner', 'sosana-sasana@yandex.com', NULL),
    ('comment-analytics-updated', 'a.s.gushchin@yandex.ru', NULL),
    ('comment-analytics-updated', 'sosana-sasana@yandex.com', NULL),
    ('comment-checklist-deleted', 'sosana-sasana@yandex.com', NULL),
    ('visibility-member-granted', 'a.s.gushchin@yandex.ru', NULL),
    ('visibility-member-granted', 'sosana-sasana@yandex.com', NULL);

INSERT INTO user_notifications (
    notification_event_id,
    recipient_user_id,
    recipient_email,
    read_at,
    created_at
)
SELECT
    events.id,
    users.id,
    NULL,
    CASE
        WHEN recipients.read_ago IS NULL THEN NULL
        ELSE CURRENT_TIMESTAMP - recipients.read_ago
    END,
    events.created_at
FROM seed_notification_recipients recipients
JOIN users ON lower(users.email) = recipients.recipient_email
JOIN seed_notification_events seed ON seed.seed_key = recipients.seed_key
JOIN notification_events events
    ON events.event_type = seed.event_type
    AND events.actor_user_id IS NOT DISTINCT FROM seed.actor_user_id
    AND events.team_id IS NOT DISTINCT FROM seed.team_id
    AND events.task_id IS NOT DISTINCT FROM seed.task_id
    AND events.comment_id IS NOT DISTINCT FROM seed.comment_id
    AND events.invitation_id IS NOT DISTINCT FROM seed.invitation_id
    AND events.subject_user_id IS NOT DISTINCT FROM seed.subject_user_id
    AND events.payload = seed.payload
ON CONFLICT (notification_event_id, recipient_user_id) DO UPDATE SET
    read_at = EXCLUDED.read_at,
    created_at = EXCLUDED.created_at;

COMMIT;
