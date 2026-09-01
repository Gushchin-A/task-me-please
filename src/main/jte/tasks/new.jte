@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param Long selectedTeamId
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Создать задачу",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    headerContext = null,
    content = @`
        <section class="workspace-page form-page task-create-page">
            <a class="back-link team-create-back-link" href="/tasks" data-history-back>
                <svg
                    aria-hidden="true"
                    width="16"
                    height="16"
                    viewBox="0 0 16 16"
                    fill="none"
                >
                    <path
                        d="M6.75 3.25 2 8m0 0 4.75 4.75M2 8h12"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    ></path>
                </svg>
                <span>Вернуться назад</span>
            </a>

            <header class="page-header page-header-compact team-create-header">
                <div>
                    <h1>Создание новой задачи</h1>
                    <p class="form-page-description">
                        Придумайте название и опишите задачу.<br>
                        Выберите команду, исполнителя, дедлайн и подходящий тег.
                    </p>
                </div>
            </header>

            @if(teams.isEmpty())
                <div class="task-create-unavailable">
                    <p>Сначала создайте команду.</p>

                    <a class="button button-primary" href="/teams/new">
                        Создать команду
                    </a>
                </div>
            @else
                <form class="task-create-form" method="post" action="/tasks">
                    <input
                        type="hidden"
                        name="${_csrf.getParameterName()}"
                        value="${_csrf.getToken()}"
                    >

                    <div class="form-field task-create-field task-team-field">
                        <label id="task-team-label">
                            Команда <span class="required-marker" aria-hidden="true">*</span>
                        </label>

                        <input
                            type="hidden"
                            name="teamId"
                            value="${selectedTeamId}"
                        >

                        <div class="primer-select" data-select="team">
                            <button
                                class="primer-select-trigger"
                                type="button"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                                aria-required="true"
                                aria-labelledby="task-team-label task-team-value"
                            >
                                <span
                                    class="primer-select-value"
                                    id="task-team-value"
                                >
                                    @for(Team team : teams)
                                        @if(selectedTeamId != null && selectedTeamId.equals(team.getId()))
                                            ${team.getName()}
                                        @endif
                                    @endfor
                                </span>

                                <svg
                                    aria-hidden="true"
                                    width="16"
                                    height="16"
                                    viewBox="0 0 16 16"
                                    fill="currentColor"
                                >
                                    <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                </svg>
                            </button>

                            <div class="primer-select-panel" hidden>
                                <div class="primer-select-heading">Выберите команду</div>

                                <div
                                    class="primer-select-options"
                                    role="listbox"
                                    aria-labelledby="task-team-label"
                                >
                                    @for(Team team : teams)
                                        <button
                                            class="primer-select-option"
                                            type="button"
                                            role="option"
                                            aria-selected="${selectedTeamId != null
                                                && selectedTeamId.equals(team.getId())}"
                                            data-team-id="${team.getId()}"
                                        >
                                            <span class="primer-select-check" aria-hidden="true">
                                                <svg
                                                    width="16"
                                                    height="16"
                                                    viewBox="0 0 16 16"
                                                    fill="currentColor"
                                                >
                                                    <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                </svg>
                                            </span>
                                            <span class="primer-select-option-copy">
                                                ${team.getName()}
                                            </span>
                                        </button>
                                    @endfor
                                </div>
                            </div>
                        </div>
                    </div>

                    @if(tags.isEmpty())
                        <div class="task-create-unavailable">
                            <p>
                                В выбранной команде нет тегов.
                                Создать задачу пока невозможно.
                            </p>
                        </div>
                    @else
                        <div class="form-field task-create-field task-assignee-field">
                            <label id="task-assignee-label">
                                Исполнитель <span class="required-marker" aria-hidden="true">*</span>
                            </label>

                            <input
                                id="task-assignee"
                                type="hidden"
                                name="assigneeId"
                                value="${members.getFirst().id().toString()}"
                            >

                            <div class="primer-select" data-select="assignee">
                                <button
                                    class="primer-select-trigger"
                                    type="button"
                                    aria-haspopup="listbox"
                                    aria-expanded="false"
                                    aria-required="true"
                                    aria-labelledby="task-assignee-label task-assignee-value"
                                >
                                    <span class="participant-avatar" data-selected-avatar>
                                        ${members.getFirst().initial()}
                                    </span>
                                    <span
                                        class="primer-select-value"
                                        id="task-assignee-value"
                                        title="${members.getFirst().displayName()}"
                                    >
                                        ${members.getFirst().shortDisplayName()}
                                    </span>

                                    <svg
                                        aria-hidden="true"
                                        width="16"
                                        height="16"
                                        viewBox="0 0 16 16"
                                        fill="currentColor"
                                    >
                                        <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                    </svg>
                                </button>

                                <div class="primer-select-panel" hidden>
                                    <div class="primer-select-heading">Выберите исполнителя</div>

                                    <div
                                        class="primer-select-options"
                                        role="listbox"
                                        aria-labelledby="task-assignee-label"
                                    >
                                        @for(TaskParticipantView member : members)
                                            <button
                                                class="primer-select-option"
                                                type="button"
                                                role="option"
                                                aria-selected="${member.id().equals(members.getFirst().id())}"
                                                data-value="${member.id().toString()}"
                                                data-label="${member.shortDisplayName()}"
                                                data-full-label="${member.displayName()}"
                                                data-avatar="${member.initial()}"
                                            >
                                                <span class="primer-select-check" aria-hidden="true">
                                                    <svg
                                                        width="16"
                                                        height="16"
                                                        viewBox="0 0 16 16"
                                                        fill="currentColor"
                                                    >
                                                        <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                    </svg>
                                                </span>
                                                <span class="participant-avatar">${member.initial()}</span>
                                                <span class="primer-select-option-copy">
                                                    ${member.displayName()}
                                                </span>
                                            </button>
                                        @endfor
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="form-field task-create-field task-title-field">
                            <label for="task-title">
                                Название задачи <span class="required-marker" aria-hidden="true">*</span>
                            </label>

                            <input
                                id="task-title"
                                type="text"
                                name="title"
                                maxlength="100"
                                required
                                aria-describedby="task-title-count"
                            >

                            <p class="form-help task-title-count" id="task-title-count">
                                <span id="task-title-length">0</span> / 100 символов
                            </p>
                        </div>

                        <div class="form-field task-create-field task-description-field">
                            <label id="task-description-label">Описание</label>

                            <div
                                class="task-description-editor"
                                aria-labelledby="task-description-label"
                            >
                                <div class="task-editor-toolbar">
                                    <div
                                        class="task-editor-tabs"
                                        role="tablist"
                                        aria-label="Режим описания"
                                    >
                                        <button
                                            class="task-editor-tab task-editor-tab-active"
                                            type="button"
                                            role="tab"
                                            aria-selected="true"
                                            aria-controls="task-description"
                                            tabindex="0"
                                            data-editor-mode="write"
                                        >
                                            Ввод
                                        </button>
                                        <button
                                            class="task-editor-tab"
                                            type="button"
                                            role="tab"
                                            aria-selected="false"
                                            aria-controls="task-description-preview"
                                            tabindex="-1"
                                            data-editor-mode="preview"
                                        >
                                            Предпросмотр
                                        </button>
                                    </div>

                                    <div
                                        class="task-editor-actions"
                                        role="toolbar"
                                        aria-label="Форматирование"
                                    >
                                        <button
                                            type="button"
                                            data-format="bold"
                                            aria-label="Жирный"
                                        >
                                            <strong>B</strong>
                                        </button>
                                        <button
                                            type="button"
                                            data-format="italic"
                                            aria-label="Курсив"
                                        >
                                            <em>I</em>
                                        </button>
                                        <button
                                            type="button"
                                            data-format="quote"
                                            aria-label="Цитата"
                                        >
                                            <svg
                                                aria-hidden="true"
                                                width="16"
                                                height="16"
                                                viewBox="0 0 16 16"
                                                fill="currentColor"
                                            >
                                                <path d="M1.75 2.5h10.5a.75.75 0 0 1 0 1.5H1.75a.75.75 0 0 1 0-1.5Zm4 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2.5 7.75v6a.75.75 0 0 1-1.5 0v-6a.75.75 0 0 1 1.5 0Z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>

                                <textarea
                                    id="task-description"
                                    name="description"
                                    rows="8"
                                    placeholder="Опишите, что нужно сделать…"
                                ></textarea>

                                <div
                                    id="task-description-preview"
                                    class="task-description-preview"
                                    data-description-preview
                                    hidden
                                ></div>
                            </div>

                            <button
                                class="task-file-placeholder"
                                type="button"
                                aria-disabled="true"
                                title="Еще не реализовано :("
                            >
                                <svg
                                    aria-hidden="true"
                                    width="16"
                                    height="16"
                                    viewBox="0 0 16 16"
                                    fill="currentColor"
                                >
                                    <path d="M12.212 3.02a1.753 1.753 0 0 0-2.478.003l-5.83 5.83a3.007 3.007 0 0 0-.88 2.127c0 .795.315 1.551.88 2.116.567.567 1.333.89 2.126.89.79 0 1.548-.321 2.116-.89l5.48-5.48a.75.75 0 0 1 1.061 1.06l-5.48 5.48a4.492 4.492 0 0 1-3.177 1.33c-1.2 0-2.345-.487-3.187-1.33a4.483 4.483 0 0 1-1.32-3.177c0-1.195.475-2.341 1.32-3.186l5.83-5.83a3.25 3.25 0 0 1 5.553 2.297c0 .863-.343 1.691-.953 2.301L7.439 12.39c-.375.377-.884.59-1.416.593a1.998 1.998 0 0 1-1.412-.593 1.992 1.992 0 0 1 0-2.828l5.48-5.48a.75.75 0 0 1 1.06 1.06l-5.48 5.48a.492.492 0 0 0 0 .707.499.499 0 0 0 .352.154.51.51 0 0 0 .356-.154l5.833-5.827a1.755 1.755 0 0 0 0-2.481Z"></path>
                                </svg>
                                <span>Добавить файлы</span>
                            </button>
                        </div>

                        <div class="form-field task-create-field task-deadline-field">
                            <label for="task-deadline">
                                Дедлайн <span class="required-marker" aria-hidden="true">*</span>
                            </label>

                            <input
                                id="task-deadline"
                                type="date"
                                name="deadlineDate"
                                required
                            >
                        </div>

                        <div class="form-field task-create-field task-tag-field">
                            <label id="task-tag-label">
                                Тег <span class="required-marker" aria-hidden="true">*</span>
                            </label>

                            <input
                                id="task-tag"
                                type="hidden"
                                name="tagId"
                                value="${tags.getFirst().getId()}"
                            >

                            <div class="primer-select" data-select="tag">
                                <button
                                    class="primer-select-trigger"
                                    type="button"
                                    aria-haspopup="listbox"
                                    aria-expanded="false"
                                    aria-required="true"
                                    aria-labelledby="task-tag-label task-tag-value"
                                >
                                    <span class="primer-select-value" id="task-tag-value">
                                        ${tags.getFirst().getName()}
                                    </span>

                                    <svg
                                        aria-hidden="true"
                                        width="16"
                                        height="16"
                                        viewBox="0 0 16 16"
                                        fill="currentColor"
                                    >
                                        <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                    </svg>
                                </button>

                                <div class="primer-select-panel" hidden>
                                    <div class="primer-select-heading">Выберите тег</div>

                                    <div
                                        class="primer-select-options"
                                        role="listbox"
                                        aria-labelledby="task-tag-label"
                                    >
                                        @for(TeamTag tag : tags)
                                            <button
                                                class="primer-select-option"
                                                type="button"
                                                role="option"
                                                aria-selected="${tag.getId().equals(tags.getFirst().getId())}"
                                                data-value="${tag.getId()}"
                                                data-label="${tag.getName()}"
                                                data-full-label="${tag.getName()}"
                                            >
                                                <span class="primer-select-check" aria-hidden="true">
                                                    <svg
                                                        width="16"
                                                        height="16"
                                                        viewBox="0 0 16 16"
                                                        fill="currentColor"
                                                    >
                                                        <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                    </svg>
                                                </span>
                                                <span class="primer-select-option-copy">
                                                    ${tag.getName()}
                                                </span>
                                            </button>
                                        @endfor
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="team-create-submit" data-keyboard-action-group>
                            <a class="button button-secondary" href="/tasks">
                                Отменить
                            </a>
                            <button class="button button-primary" type="submit">
                                Создать задачу
                            </button>
                        </div>
                    @endif
                </form>
            @endif
        </section>

        <script>
            const titleInput = document.getElementById('task-title');
            const titleLength = document.getElementById('task-title-length');

            if (titleInput && titleLength) {
                titleInput.addEventListener('input', function () {
                    titleLength.textContent = titleInput.value.length;
                });
            }

            function closeTaskSelects(exceptPanel) {
                document
                    .querySelectorAll('.primer-select-panel:not([hidden])')
                    .forEach(function (panel) {
                        if (panel !== exceptPanel) {
                            panel.hidden = true;
                            panel.previousElementSibling.setAttribute('aria-expanded', 'false');
                        }
                    });
            }

            document
                .querySelectorAll('.primer-select-trigger')
                .forEach(function (trigger) {
                    trigger.addEventListener('click', function (event) {
                        event.stopPropagation();

                        const panel = trigger.nextElementSibling;
                        const shouldOpen = panel.hidden;

                        closeTaskSelects(panel);
                        panel.hidden = !shouldOpen;
                        trigger.setAttribute('aria-expanded', String(shouldOpen));

                        if (shouldOpen) {
                            const selectedOption = panel.querySelector('[aria-selected="true"]');
                            const firstOption = panel.querySelector('[role="option"]');

                            (selectedOption || firstOption)?.focus();
                        }
                    });
                });

            document
                .querySelectorAll('.primer-select-option')
                .forEach(function (option) {
                    option.addEventListener('click', function () {
                        if (option.dataset.teamId) {
                            window.location.href = '/tasks/new?teamId=' + option.dataset.teamId;
                            return;
                        }

                        const select = option.closest('.primer-select');
                        const field = select.closest('.task-create-field');
                        const hiddenInput = field.querySelector('input[type="hidden"]');
                        const value = select.querySelector('.primer-select-value');
                        const avatar = select.querySelector('[data-selected-avatar]');

                        select
                            .querySelectorAll('.primer-select-option')
                            .forEach(function (item) {
                                item.setAttribute('aria-selected', 'false');
                            });

                        option.setAttribute('aria-selected', 'true');
                        hiddenInput.value = option.dataset.value;
                        value.textContent = option.dataset.label;
                        value.title = option.dataset.fullLabel;

                        if (avatar) {
                            avatar.textContent = option.dataset.avatar;
                        }

                        select.querySelector('.primer-select-panel').hidden = true;
                        select.querySelector('.primer-select-trigger')
                            .setAttribute('aria-expanded', 'false');
                    });
                });

            document.addEventListener('click', function () {
                closeTaskSelects();
            });

            document.addEventListener('keydown', function (event) {
                if (event.key === 'Escape') {
                    closeTaskSelects();
                }
            });

            const descriptionInput = document.getElementById('task-description');
            const descriptionPreview = document.querySelector('[data-description-preview]');
            const descriptionEditor = descriptionInput.closest('.task-description-editor');

            document
                .querySelectorAll('[data-editor-mode]')
                .forEach(function (tab) {
                    tab.addEventListener('click', function () {
                        const writeMode = tab.dataset.editorMode === 'write';

                        document
                            .querySelectorAll('[data-editor-mode]')
                            .forEach(function (item) {
                                const selected = item === tab;

                                item.classList.toggle('task-editor-tab-active', selected);
                                item.setAttribute('aria-selected', String(selected));
                                item.setAttribute('tabindex', selected ? '0' : '-1');
                            });

                        const editorHeight = descriptionInput.offsetHeight + 16;

                        descriptionInput.hidden = !writeMode;
                        descriptionPreview.hidden = writeMode;
                        descriptionEditor.classList.toggle(
                            'task-description-editor-preview',
                            !writeMode
                        );

                        if (!writeMode) {
                            descriptionPreview.innerHTML = renderLimitedMarkdown(descriptionInput.value);
                            descriptionPreview.style.height = editorHeight + 'px';
                        }
                    });
                });

            document
                .querySelectorAll('[data-format]')
                .forEach(function (button) {
                    button.addEventListener('click', function () {
                        formatEditorSelection(descriptionInput, button.dataset.format);
                    });
                });

            document
                .querySelectorAll('.task-file-placeholder')
                .forEach(function (button) {
                    button.addEventListener('click', function (event) {
                        event.preventDefault();
                    });
                });
        </script>
    `
)
