CREATE TABLE team_tags (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_team_tags_team_id
        FOREIGN KEY (team_id) REFERENCES teams (id)
);

CREATE INDEX idx_team_tags_team_id
    ON team_tags (team_id);

CREATE UNIQUE INDEX uq_team_tags_team_id_normalized_name_active
    ON team_tags (team_id, normalized_name)
    WHERE is_deleted = FALSE;

ALTER TABLE tasks
ADD COLUMN tag_id BIGINT;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Кинопоиск',
    'кинопоиск',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Музыка',
    'музыка',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Книги',
    'книги',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Плюс',
    'плюс',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Спорт',
    'спорт',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Радио',
    'радио',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Афиша',
    'афиша',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Гейминг',
    'гейминг',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Несколько сервисов',
    'несколько сервисов',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

INSERT INTO team_tags (
    team_id,
    name,
    normalized_name,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    id,
    'Без сервиса',
    'без сервиса',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM teams;

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'кинопоиск'
)
WHERE category = 'KINOPOISK';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'музыка'
)
WHERE category = 'MUSIC';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'книги'
)
WHERE category = 'KNIGI';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'плюс'
)
WHERE category = 'PLUS';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'спорт'
)
WHERE category = 'SPORT';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'радио'
)
WHERE category = 'RADIO';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'афиша'
)
WHERE category = 'AFISHA';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'гейминг'
)
WHERE category = 'GAMING';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'несколько сервисов'
)
WHERE category = 'COMBO_SERVICES';

UPDATE tasks
SET tag_id = (
    SELECT team_tags.id
    FROM team_tags
    WHERE team_tags.team_id = tasks.team_id
      AND team_tags.normalized_name = 'без сервиса'
)
WHERE category = 'NO_SERVICES';

ALTER TABLE tasks
ALTER COLUMN tag_id SET NOT NULL;

ALTER TABLE tasks
ALTER COLUMN tag_id DROP NOT NULL;

CREATE INDEX idx_tasks_tag_id
    ON tasks (tag_id);