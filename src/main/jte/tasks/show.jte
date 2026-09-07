@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.CommentView
@import dev.gushchin.taskmanager.view.TaskView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TaskView task
@param Team team
@param List<Team> navigationTeams
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param List<CommentView> comments
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String successMessage

@template.layout.page(
    title = task.title(),
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    successMessage = successMessage,
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = task.archived() ? "archive" : "tasks"
        )

        <div class="task-detail-layout" data-task-detail>
        <section class="task-detail-main">
            <a
                class="back-link team-create-back-link"
                href="/teams/${team.getId()}${task.archived() ? "/archive" : ""}"
                data-history-back
                data-history-back-scope="/tasks/${task.id()}"
            >
                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M7.78 12.53a.75.75 0 0 1-1.06 0l-4-4a.75.75 0 0 1 0-1.06l4-4a.75.75 0 0 1 1.06 1.06L5.06 7.25h7.69a.75.75 0 0 1 0 1.5H5.06l2.72 2.72a.75.75 0 0 1 0 1.06Z"></path>
                </svg>
                Вернуться назад
            </a>

            <div class="task-title-row">
                <h1>${task.title()}</h1>

                @if(task.canUpdateTask() && !task.archived())
                    <button
                        class="task-detail-icon-button"
                        type="button"
                        aria-label="Изменить название задачи"
                        data-tooltip="Изменить название задачи"
                        data-task-title-edit-open
                    >
                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                            <path d="M11.013 1.427a1.75 1.75 0 0 1 2.474 0l1.086 1.086a1.75 1.75 0 0 1 0 2.474l-8.5 8.5a.75.75 0 0 1-.33.194l-3.5 1a.75.75 0 0 1-.927-.927l1-3.5a.75.75 0 0 1 .194-.33Zm1.414 1.06a.25.25 0 0 0-.354 0L3.7 10.86l-.62 2.17 2.17-.62 8.373-8.373a.25.25 0 0 0 0-.354Z"></path>
                        </svg>
                    </button>
                @endif
            </div>

            @if(task.canUpdateTask() && !task.archived())
                <form
                    class="task-title-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    hidden
                    data-task-title-edit-form
                    data-keyboard-action-group
                    data-submit-loading
                >
                    <input
                        type="hidden"
                        name="${_csrf.getParameterName()}"
                        value="${_csrf.getToken()}"
                    >

                    <input
                        type="hidden"
                        name="tagId"
                        value="${task.tagId()}"
                    >

                    <input
                        type="hidden"
                        name="assigneeId"
                        value="${task.assigneeId().toString()}"
                    >

                    <input type="hidden" name="returnTo" value="task">

                    @if(task.deadlineAt() != null)
                        <input
                            type="hidden"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif

                    <input
                        id="task-detail-title-${task.id()}"
                        type="text"
                        name="title"
                        value="${task.title()}"
                        maxlength="100"
                        aria-label="Название задачи"
                        aria-describedby="task-detail-title-count"
                        data-original-value="${task.title()}"
                        required
                    >
                    <button class="button button-secondary" type="button" data-task-title-edit-cancel>Отменить</button>
                    <span class="team-settings-action-tooltip" data-tooltip="Начните редактировать название">
                        <button
                            class="button button-primary"
                            type="submit"
                            disabled
                            data-task-title-edit-submit
                            data-loading-text="Сохранение..."
                        >Сохранить</button>
                    </span>
                    <p class="form-help task-title-count" id="task-detail-title-count">
                        <span data-task-detail-title-length>${task.title().length()}</span> / 100 символов
                    </p>
                </form>
            @endif

            <div class="task-detail-summary">
                <span class="task-detail-summary-statuses">
                    @if(task.archived())
                        <span class="task-detail-status task-detail-status-archive">Архив</span>
                        @if(task.status() == TaskStatus.DONE)
                            <span
                                class="task-detail-status task-detail-archive-outcome-resolved"
                                data-tooltip="Задача решена и перенесена в архив"
                            >Решена</span>
                        @else
                            <span class="task-detail-status task-detail-archive-outcome-deleted">Удалена</span>
                        @endif
                    @else
                        <span class="task-detail-status task-detail-status-${task.statusCssClass()}">${task.statusText()}</span>
                    @endif
                </span>
                <span>
                    <strong>${task.authorName()}</strong> создал задачу ${task.createdAtText()}
                </span>
            </div>
        </section>

        <aside class="task-parameters-panel" aria-label="Параметры задачи" data-task-parameters>
            <div class="task-parameter-row task-parameter-status-row">
                <div class="task-parameter-heading">
                    <span>Статус</span>

                    @if(task.canUpdateStatus() && !task.archived())
                        <button class="task-detail-icon-button" type="button" aria-label="Изменить параметры задачи" data-tooltip="Изменить параметры задачи" data-task-parameters-edit-open>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                <path d="M8 0a8.2 8.2 0 0 1 .701.031C9.444.095 9.99.645 10.16 1.29l.288 1.107c.018.066.079.158.212.224.231.114.454.243.668.386.123.082.233.09.299.071l1.103-.303c.644-.176 1.392.021 1.82.63.27.385.506.792.704 1.218.315.675.111 1.422-.364 1.891l-.814.806c-.049.048-.098.147-.088.294.016.257.016.515 0 .772-.01.147.038.246.088.294l.814.806c.475.469.679 1.216.364 1.891a7.977 7.977 0 0 1-.704 1.217c-.428.61-1.176.807-1.82.63l-1.102-.302c-.067-.019-.177-.011-.3.071a5.909 5.909 0 0 1-.668.386c-.133.066-.194.158-.211.224l-.29 1.106c-.168.646-.715 1.196-1.458 1.26a8.006 8.006 0 0 1-1.402 0c-.743-.064-1.289-.614-1.458-1.26l-.289-1.106c-.018-.066-.079-.158-.212-.224a5.738 5.738 0 0 1-.668-.386c-.123-.082-.233-.09-.299-.071l-1.103.303c-.644.176-1.392-.021-1.82-.63a8.12 8.12 0 0 1-.704-1.218c-.315-.675-.111-1.422.363-1.891l.815-.806c.05-.048.098-.147.088-.294a6.214 6.214 0 0 1 0-.772c.01-.147-.038-.246-.088-.294l-.815-.806C.635 6.045.431 5.298.746 4.623a7.92 7.92 0 0 1 .704-1.217c.428-.61 1.176-.807 1.82-.63l1.102.302c.067.019.177.011.3-.071.214-.143.437-.272.668-.386.133-.066.194-.158.211-.224l.29-1.106C6.009.645 6.556.095 7.299.03 7.53.01 7.764 0 8 0Zm-.571 1.525c-.036.003-.108.036-.137.146l-.289 1.105c-.147.561-.549.967-.998 1.189-.173.086-.34.183-.5.29-.417.278-.97.423-1.529.27l-1.103-.303c-.109-.03-.175.016-.195.045-.22.312-.412.644-.573.99-.014.031-.021.11.059.19l.815.806c.411.406.562.957.53 1.456a4.709 4.709 0 0 0 0 .582c.032.499-.119 1.05-.53 1.456l-.815.806c-.081.08-.073.159-.059.19.162.346.353.677.573.989.02.03.085.076.195.046l1.102-.303c.56-.153 1.113-.008 1.53.27.161.107.328.204.501.29.447.222.85.629.997 1.189l.289 1.105c.029.109.101.143.137.146a6.6 6.6 0 0 0 1.142 0c.036-.003.108-.036.137-.146l.289-1.105c.147-.561.549-.967.998-1.189.173-.086.34-.183.5-.29.417-.278.97-.423 1.529-.27l1.103.303c.109.029.175-.016.195-.045.22-.313.411-.644.573-.99.014-.031.021-.11-.059-.19l-.815-.806c-.411-.406-.562-.957-.53-1.456a4.709 4.709 0 0 0 0-.582c-.032-.499.119-1.05.53-1.456l.815-.806c.081-.08.073-.159.059-.19a6.464 6.464 0 0 0-.573-.989c-.02-.03-.085-.076-.195-.046l-1.102.303c-.56.153-1.113.008-1.53-.27a4.44 4.44 0 0 0-.501-.29c-.447-.222-.85-.629-.997-1.189l-.289-1.105c-.029-.11-.101-.143-.137-.146a6.6 6.6 0 0 0-1.142 0ZM11 8a3 3 0 1 1-6 0 3 3 0 0 1 6 0ZM9.5 8a1.5 1.5 0 1 0-3.001.001A1.5 1.5 0 0 0 9.5 8Z"></path>
                            </svg>
                        </button>
                    @endif
                </div>
                <span class="task-parameter-value task-parameter-statuses" data-task-parameter-value>
                    @if(task.archived())
                        <span class="task-detail-status task-detail-status-archive">Архив</span>
                    @endif
                    <span class="task-detail-status task-detail-status-${task.archived() ? "archived-original" : task.statusCssClass()}">${task.statusText()}</span>
                </span>
                <form method="post" action="/tasks/${task.id()}/status" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select name="status" data-original-value="${task.status().name()}" disabled="${!task.canUpdateStatus()}">
                        @for(TaskStatus status : TaskStatus.values())
                            <option value="${status.name()}" selected="${status == task.status()}">${TaskView.statusText(status)}</option>
                        @endfor
                    </select>
                </form>
            </div>

            <div
                class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}"
                data-tooltip="${!task.canUpdateTask() ? "Редактирование доступно только автору задачи" : null}"
                data-tooltip-disabled="${!task.canUpdateTask() ? true : null}"
                data-task-read-only-tooltip="${!task.canUpdateTask() ? true : null}"
            >
                <span class="task-parameter-heading">Дедлайн</span>
                <span class="task-parameter-value" data-task-parameter-value>${task.deadlineText()}</span>
                <form method="post" action="/tasks/${task.id()}/deadline" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <input type="date" name="deadlineDate" value="${task.deadlineAt() == null ? "" : task.deadlineAt().toString().substring(0, 10)}" data-original-value="${task.deadlineAt() == null ? "" : task.deadlineAt().toString().substring(0, 10)}" disabled="${!task.canUpdateTask()}">
                </form>
            </div>

            <div class="task-parameter-row task-parameter-team-row">
                <span class="task-parameter-heading">Команда</span>
                <a class="task-parameter-value task-parameter-team" href="/teams/${team.getId()}">
                    ${team.getName()}
                </a>
            </div>

            <div
                class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}"
                data-tooltip="${!task.canUpdateTask() ? "Редактирование доступно только автору задачи" : null}"
                data-tooltip-disabled="${!task.canUpdateTask() ? true : null}"
                data-task-read-only-tooltip="${!task.canUpdateTask() ? true : null}"
            >
                <span class="task-parameter-heading">Автор</span>
                <span class="task-parameter-value task-parameter-person ${task.authorRemovedFromTeam() ? "task-card-former-member" : ""}" title="${task.participants().author().removedFromTeamTitle()}" data-task-parameter-value>
                    <span class="task-parameter-avatar" aria-hidden="true">${task.participants().author().initial()}</span>
                    <span>${task.authorName()}</span>
                </span>
                <form method="post" action="/tasks/${task.id()}/author" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select
                        name="authorId"
                        data-original-value="${task.authorId().toString()}"
                        data-participant-select
                        data-tooltip="${task.authorChangeWarningText()}"
                        data-author-change-warning="${task.showAuthorChangeWarning() ? true : null}"
                        data-current-author-id="${task.showAuthorChangeWarning() ? task.authorId().toString() : null}"
                        data-access-warning="${task.showAuthorChangeWarning()
                            ? "После смены автора вы потеряете доступ к этой задаче"
                            : null}"
                        data-edit-warning="${task.showAuthorChangeWarning()
                            ? "После смены автора вы не сможете редактировать задачу"
                            : null}"
                        disabled="${!task.canUpdateTask()}"
                    >
                        <option value="${task.authorId().toString()}" selected>${task.authorName()}</option>
                        @for(TaskParticipantView member : members)
                            @if(!member.id().equals(task.authorId()))
                                <option value="${member.id().toString()}">${member.name()}</option>
                            @endif
                        @endfor
                    </select>
                </form>
            </div>

            <div
                class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}"
                data-tooltip="${!task.canUpdateTask() ? "Редактирование доступно только автору задачи" : null}"
                data-tooltip-disabled="${!task.canUpdateTask() ? true : null}"
                data-task-read-only-tooltip="${!task.canUpdateTask() ? true : null}"
            >
                <span class="task-parameter-heading">Исполнитель</span>
                <span class="task-parameter-value task-parameter-person ${task.assigneeRemovedFromTeam() ? "task-card-former-member" : ""}" title="${task.participants().assignee().removedFromTeamTitle()}" data-task-parameter-value>
                    <span class="task-parameter-avatar" aria-hidden="true">${task.participants().assignee().initial()}</span>
                    <span>${task.assigneeName()}</span>
                </span>
                <form method="post" action="/tasks/${task.id()}/assignee" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select name="assigneeId" data-original-value="${task.assigneeId().toString()}" data-participant-select disabled="${!task.canUpdateTask()}">
                        <option value="${task.assigneeId().toString()}" selected>${task.assigneeName()}</option>
                        @for(TaskParticipantView member : members)
                            @if(!member.id().equals(task.assigneeId()))
                                <option value="${member.id().toString()}">${member.name()}</option>
                            @endif
                        @endfor
                    </select>
                </form>
            </div>

            <div
                class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}"
                data-tooltip="${!task.canUpdateTask() ? "Редактирование доступно только автору задачи" : null}"
                data-tooltip-disabled="${!task.canUpdateTask() ? true : null}"
                data-task-read-only-tooltip="${!task.canUpdateTask() ? true : null}"
            >
                <span class="task-parameter-heading">Тег</span>
                <span class="task-parameter-value task-detail-tag" data-task-parameter-value>${task.tagName()}</span>
                <form method="post" action="/tasks/${task.id()}/tag" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select name="tagId" data-original-value="${task.tagId()}" disabled="${!task.canUpdateTask()}">
                        @for(TeamTag tag : tags)
                            <option value="${tag.getId()}" selected="${tag.getId().equals(task.tagId())}">${tag.getName()}</option>
                        @endfor
                    </select>
                </form>
            </div>

            <div class="task-parameters-actions" hidden data-task-parameters-actions data-keyboard-action-group>
                <button class="button button-secondary" type="button" data-task-parameters-cancel>Отменить</button>
                <button class="button button-primary" type="button" disabled data-task-parameters-save>Сохранить</button>
            </div>

            @if(task.archived())
                @if(task.canRestore())
                    <form class="task-parameters-state-action" method="post" action="/tasks/${task.id()}/restore">
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <input type="hidden" name="returnTo" value="task">
                        <button class="task-parameters-state-link" type="submit">Вернуть из архива</button>
                    </form>
                @endif
            @elseif(task.canArchive())
                <form class="task-parameters-state-action" method="post" action="/tasks/${task.id()}/archive" data-task-parameters-state-action>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="team">

                    @if(task.status() == TaskStatus.DONE)
                        <button class="task-parameters-state-link" type="submit">Перенести в архив</button>
                    @else
                        <button class="task-parameters-state-link task-parameters-state-link-danger" type="submit">Удалить задачу</button>
                    @endif
                </form>
            @endif
        </aside>

        <section class="task-conversation" aria-label="Обсуждение задачи">
            <div class="task-conversation-thread ${comments.isEmpty() ? "task-conversation-thread-single" : ""}">
            <article id="task-description" class="comment-item task-description-item task-description-status-${task.archived() ? "not-relevant" : task.statusCssClass()}">
                <span class="comment-avatar" aria-hidden="true">${task.participants().author().initial()}</span>

                <div class="comment-card">
                    <header class="comment-header">
                        <div class="comment-header-copy">
                            <strong data-tooltip="${task.participants().author().commentDisplayNameTooltip()}">${task.participants().author().commentDisplayName()}</strong>
                            <span class="comment-header-meta">добавил описание задачи · ${task.updatedAtText()}</span>
                        </div>

                        <details class="task-card-actions comment-actions">
                            <summary class="task-card-menu" aria-label="Действия с описанием">
                                <svg aria-hidden="true" viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
                                    <path d="M2 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path>
                                </svg>
                            </summary>
                            <div class="task-card-actions-panel">
                                <button class="task-card-action-option" type="button" data-task-anchor-copy="task-description">Скопировать ссылку</button>

                                @if(task.canUpdateTask())
                                    <div class="task-card-actions-divider"></div>
                                    <button class="task-card-action-option" type="button" data-description-edit-open>Изменить</button>
                                @endif
                            </div>
                        </details>
                    </header>

                    <div class="comment-body" data-description-view>@if(task.description() == null || task.description().isBlank())Описание пока не добавлено.@else$unsafe{task.descriptionHtml()}@endif</div>

                    @if(task.canUpdateTask())
                        <form class="comment-edit-form task-description-edit-form" method="post" action="/tasks/${task.id()}/edit" hidden data-description-edit-form>
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                            <input type="hidden" name="title" value="${task.title()}">
                            <input type="hidden" name="tagId" value="${task.tagId()}">
                            <input type="hidden" name="assigneeId" value="${task.assigneeId().toString()}">
                            <input type="hidden" name="returnTo" value="task">

                            @if(task.deadlineAt() != null)
                                <input type="hidden" name="deadlineDate" value="${task.deadlineAt().toString().substring(0, 10)}">
                            @endif

                            <div class="task-description-editor task-comment-editor">
                                <div class="task-editor-toolbar">
                                    <div class="task-editor-tabs" role="tablist" aria-label="Режим описания">
                                        <button class="task-editor-tab task-editor-tab-active" type="button" role="tab" aria-selected="true" aria-controls="task-description-edit-${task.id()}" tabindex="0" data-editor-mode="write">Ввод</button>
                                        <button class="task-editor-tab" type="button" role="tab" aria-selected="false" aria-controls="task-description-edit-preview-${task.id()}" tabindex="-1" data-editor-mode="preview">Предпросмотр</button>
                                    </div>

                                    <div class="task-editor-actions" role="toolbar" aria-label="Форматирование">
                                        <button type="button" data-format="bold" aria-label="Жирный"><strong>B</strong></button>
                                        <button type="button" data-format="italic" aria-label="Курсив"><em>I</em></button>
                                        <button type="button" data-format="quote" aria-label="Цитата">
                                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                <path d="M1.75 2.5h10.5a.75.75 0 0 1 0 1.5H1.75a.75.75 0 0 1 0-1.5Zm4 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2.5 7.75v6a.75.75 0 0 1-1.5 0v-6a.75.75 0 0 1 1.5 0Z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>

                                <textarea id="task-description-edit-${task.id()}" name="description" rows="8" data-original-value="@if(task.description() != null)${task.description()}@endif" required>@if(task.description() != null)${task.description()}@endif</textarea>
                                <div id="task-description-edit-preview-${task.id()}" class="task-description-preview" data-description-preview hidden></div>
                            </div>
                            <button class="task-file-placeholder" type="button" aria-disabled="true" title="Еще не реализовано :(">
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="M12.212 3.02a1.753 1.753 0 0 0-2.478.003l-5.83 5.83a3.007 3.007 0 0 0-.88 2.127c0 .795.315 1.551.88 2.116.567.567 1.333.89 2.126.89.79 0 1.548-.321 2.116-.89l5.48-5.48a.75.75 0 0 1 1.061 1.06l-5.48 5.48a4.492 4.492 0 0 1-3.177 1.33c-1.2 0-2.345-.487-3.187-1.33a4.483 4.483 0 0 1-1.32-3.177c0-1.195.475-2.341 1.32-3.186l5.83-5.83a3.25 3.25 0 0 1 5.553 2.297c0 .863-.343 1.691-.953 2.301L7.439 12.39c-.375.377-.884.59-1.416.593a1.998 1.998 0 0 1-1.412-.593 1.992 1.992 0 0 1 0-2.828l5.48-5.48a.75.75 0 0 1 1.06 1.06l-5.48 5.48a.492.492 0 0 0 0 .707.499.499 0 0 0 .352.154.51.51 0 0 0 .356-.154l5.833-5.827a1.755 1.755 0 0 0 0-2.481Z"></path>
                                </svg>
                                <span>Добавить файлы</span>
                            </button>
                            <div class="comment-edit-actions" data-keyboard-action-group>
                                <button class="button button-secondary" type="button" data-description-edit-cancel>Отменить</button>
                                <button class="button button-primary" type="submit" disabled data-changed-value-submit>Сохранить</button>
                            </div>
                        </form>
                    @endif
                </div>
            </article>

            @for(CommentView comment : comments)
                <article id="comment-${comment.id()}" class="comment-item">
                    <span class="comment-avatar" aria-hidden="true">${comment.author().initial()}</span>

                    <div class="comment-card">
                        <header class="comment-header">
                            <div class="comment-header-copy">
                                <strong data-tooltip="${comment.author().commentDisplayNameTooltip()}">${comment.author().commentDisplayName()}</strong>
                                <span class="comment-header-meta">
                                    прокомментировал · ${comment.createdAtText()}@if(comment.edited())<em class="comment-edited"> · отредактировано</em>@endif
                                </span>
                            </div>

                            <details class="task-card-actions comment-actions">
                                <summary class="task-card-menu" aria-label="Действия с комментарием">
                                    <svg aria-hidden="true" viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
                                        <path d="M2 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path>
                                    </svg>
                                </summary>

                                <div class="task-card-actions-panel">
                                    <button class="task-card-action-option" type="button" data-task-anchor-copy="comment-${comment.id()}">Скопировать ссылку</button>

                                    @if(comment.canEdit())
                                        <div class="task-card-actions-divider"></div>
                                        <button class="task-card-action-option" type="button" data-comment-edit-open>Редактировать</button>

                                        <form method="post" action="/tasks/${task.id()}/comments/${comment.id()}/delete">
                                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                            <button class="task-card-action-option task-card-delete-action" type="submit">Удалить</button>
                                        </form>
                                    @endif
                                </div>
                            </details>
                        </header>

                        <div class="comment-body" data-comment-view>$unsafe{comment.messageHtml()}</div>

                        @if(comment.canEdit())
                            <form class="comment-edit-form" method="post" action="/tasks/${task.id()}/comments/${comment.id()}/edit" hidden data-comment-edit-form>
                                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                <div class="task-description-editor task-comment-editor">
                                    <div class="task-editor-toolbar">
                                        <div class="task-editor-tabs" role="tablist" aria-label="Режим комментария">
                                            <button class="task-editor-tab task-editor-tab-active" type="button" role="tab" aria-selected="true" aria-controls="comment-edit-${comment.id()}" tabindex="0" data-editor-mode="write">Ввод</button>
                                            <button class="task-editor-tab" type="button" role="tab" aria-selected="false" aria-controls="comment-edit-preview-${comment.id()}" tabindex="-1" data-editor-mode="preview">Предпросмотр</button>
                                        </div>

                                        <div class="task-editor-actions" role="toolbar" aria-label="Форматирование">
                                            <button type="button" data-format="bold" aria-label="Жирный"><strong>B</strong></button>
                                            <button type="button" data-format="italic" aria-label="Курсив"><em>I</em></button>
                                            <button type="button" data-format="quote" aria-label="Цитата">
                                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                    <path d="M1.75 2.5h10.5a.75.75 0 0 1 0 1.5H1.75a.75.75 0 0 1 0-1.5Zm4 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2.5 7.75v6a.75.75 0 0 1-1.5 0v-6a.75.75 0 0 1 1.5 0Z"></path>
                                                </svg>
                                            </button>
                                        </div>
                                    </div>

                                    <textarea id="comment-edit-${comment.id()}" name="message" rows="8" data-original-value="${comment.message()}" required>${comment.message()}</textarea>
                                    <div id="comment-edit-preview-${comment.id()}" class="task-description-preview" data-description-preview hidden></div>
                                </div>
                                <button class="task-file-placeholder" type="button" aria-disabled="true" title="Еще не реализовано :(">
                                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                        <path d="M12.212 3.02a1.753 1.753 0 0 0-2.478.003l-5.83 5.83a3.007 3.007 0 0 0-.88 2.127c0 .795.315 1.551.88 2.116.567.567 1.333.89 2.126.89.79 0 1.548-.321 2.116-.89l5.48-5.48a.75.75 0 0 1 1.061 1.06l-5.48 5.48a4.492 4.492 0 0 1-3.177 1.33c-1.2 0-2.345-.487-3.187-1.33a4.483 4.483 0 0 1-1.32-3.177c0-1.195.475-2.341 1.32-3.186l5.83-5.83a3.25 3.25 0 0 1 5.553 2.297c0 .863-.343 1.691-.953 2.301L7.439 12.39c-.375.377-.884.59-1.416.593a1.998 1.998 0 0 1-1.412-.593 1.992 1.992 0 0 1 0-2.828l5.48-5.48a.75.75 0 0 1 1.06 1.06l-5.48 5.48a.492.492 0 0 0 0 .707.499.499 0 0 0 .352.154.51.51 0 0 0 .356-.154l5.833-5.827a1.755 1.755 0 0 0 0-2.481Z"></path>
                                    </svg>
                                    <span>Добавить файлы</span>
                                </button>
                                <div class="comment-edit-actions" data-keyboard-action-group>
                                    <button class="button button-secondary" type="button" data-comment-edit-cancel>Отменить</button>
                                    <button class="button button-primary" type="submit" disabled data-changed-value-submit>Сохранить</button>
                                </div>
                            </form>
                        @endif
                    </div>
                </article>
            @endfor
            </div>

            @if(!task.archived())
            <form class="comment-create-form" method="post" action="/tasks/${task.id()}/comments" data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                <span class="comment-avatar" aria-hidden="true">${currentUser.initial()}</span>
                <div class="comment-create-content">
                    <label for="new-comment-${task.id()}">Добавить комментарий</label>
                    <div class="task-description-editor task-comment-editor">
                        <div class="task-editor-toolbar">
                            <div class="task-editor-tabs" role="tablist" aria-label="Режим комментария">
                                <button class="task-editor-tab task-editor-tab-active" type="button" role="tab" aria-selected="true" aria-controls="new-comment-${task.id()}" tabindex="0" data-editor-mode="write">Ввод</button>
                                <button class="task-editor-tab" type="button" role="tab" aria-selected="false" aria-controls="new-comment-preview-${task.id()}" tabindex="-1" data-editor-mode="preview">Предпросмотр</button>
                            </div>

                            <div class="task-editor-actions" role="toolbar" aria-label="Форматирование">
                                <button type="button" data-format="bold" aria-label="Жирный"><strong>B</strong></button>
                                <button type="button" data-format="italic" aria-label="Курсив"><em>I</em></button>
                                <button type="button" data-format="quote" aria-label="Цитата">
                                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                        <path d="M1.75 2.5h10.5a.75.75 0 0 1 0 1.5H1.75a.75.75 0 0 1 0-1.5Zm4 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2.5 7.75v6a.75.75 0 0 1-1.5 0v-6a.75.75 0 0 1 1.5 0Z"></path>
                                    </svg>
                                </button>
                            </div>
                        </div>

                        <textarea id="new-comment-${task.id()}" name="message" rows="8" placeholder="Оставьте комментарий" required></textarea>
                        <div id="new-comment-preview-${task.id()}" class="task-description-preview" data-description-preview hidden></div>
                    </div>
                    <button class="task-file-placeholder" type="button" aria-disabled="true" title="Еще не реализовано :(">
                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                            <path d="M12.212 3.02a1.753 1.753 0 0 0-2.478.003l-5.83 5.83a3.007 3.007 0 0 0-.88 2.127c0 .795.315 1.551.88 2.116.567.567 1.333.89 2.126.89.79 0 1.548-.321 2.116-.89l5.48-5.48a.75.75 0 0 1 1.061 1.06l-5.48 5.48a4.492 4.492 0 0 1-3.177 1.33c-1.2 0-2.345-.487-3.187-1.33a4.483 4.483 0 0 1-1.32-3.177c0-1.195.475-2.341 1.32-3.186l5.83-5.83a3.25 3.25 0 0 1 5.553 2.297c0 .863-.343 1.691-.953 2.301L7.439 12.39c-.375.377-.884.59-1.416.593a1.998 1.998 0 0 1-1.412-.593 1.992 1.992 0 0 1 0-2.828l5.48-5.48a.75.75 0 0 1 1.06 1.06l-5.48 5.48a.492.492 0 0 0 0 .707.499.499 0 0 0 .352.154.51.51 0 0 0 .356-.154l5.833-5.827a1.755 1.755 0 0 0 0-2.481Z"></path>
                        </svg>
                        <span>Добавить файлы</span>
                    </button>
                    <div class="comment-create-actions">
                        <button class="button button-primary" type="submit" disabled data-new-comment-submit data-loading-text="Добавление...">Добавить комментарий</button>
                    </div>
                </div>
            </form>
            @endif
        </section>

        </div>
    `
)
