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

@template.layout.page(
    title = task.title(),
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = "tasks"
        )

        <div class="task-detail-layout">
        <section class="task-detail-main">
            <a class="back-link" href="/teams/${team.getId()}">← К задачам команды</a>

            <div class="task-title-row">
                <h1 style="display: inline;">
                    ${task.title()}
                </h1>

                @if(task.canUpdateTask())
                    <button
                        class="text-action"
                        type="button"
                        onclick="document.getElementById('title-edit-form').style.display = 'block'; this.style.display = 'none'; return false;"
                    >
                        Редактировать название
                    </button>
                @endif
            </div>

            @if(task.canUpdateTask())
                <form
                    id="title-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    style="display: none; margin-top: 12px;"
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

                    @if(task.deadlineAt() != null)
                        <input
                            type="hidden"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif

                    <textarea
                        name="description"
                        style="display: none;"
                    >@if(task.description() != null)${task.description()}@endif</textarea>

                    <div>
                        <label>Название</label>

                        <input
                            type="text"
                            name="title"
                            value="${task.title()}"
                            required
                        >
                    </div>

                    <button type="submit">
                        Сохранить название
                    </button>

                    <button
                        type="button"
                        onclick="document.getElementById('title-edit-form').style.display = 'none';"
                    >
                        Отмена
                    </button>
                </form>
            @endif

            @if(task.archived())
                <p>Архив</p>
            @endif

            <div class="task-metadata-panel">
                <div>
                    <span>Статус:</span>

                    @if(task.canUpdateStatus())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/status"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            <select
                                name="status"
                                data-inline-control
                                data-original-value="${task.status().name()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                <option
                                    value="${task.status().name()}"
                                    selected
                                >
                                    ${task.status().name()}
                                </option>

                                @for(TaskStatus status : TaskStatus.values())
                                    @if(status != task.status())
                                        <option value="${status.name()}">
                                            ${status.name()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            <span
                                data-inline-actions
                                style="display: none;"
                            >
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button
                                    type="button"
                                    onclick="cancelInlineUpdate(this)"
                                >
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @else
                        <span>${task.status().name()}</span>
                    @endif
                </div>

                <div>
                    <span>Автор:</span>

                    @if(!task.canUpdateTask())
                        @if(task.authorRemovedFromTeam())
                            <span style="color: red;" title="Пользователь был удалён из команды">
                                ${task.authorName()}
                            </span>
                        @else
                            <span>${task.authorName()}</span>
                        @endif
                    @else
                        <form
                            method="post"
                            action="/tasks/${task.id()}/author"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            @if(task.showAuthorChangeWarning())
                                <span
                                    style="
                                        position: relative;
                                        display: inline-block;
                                    "
                                >
                                    <select
                                        name="authorId"
                                        data-inline-control
                                        data-original-value="${task.authorId().toString()}"
                                        onchange="showInlineSavePanel(this)"
                                        onmouseenter="document.getElementById('author-warning-${task.id()}').style.display = 'block';"
                                        onmouseleave="document.getElementById('author-warning-${task.id()}').style.display = 'none';"
                                        style="${task.participants().author().removedFromTeamStyle()}"
                                        title="${task.participants().author().removedFromTeamTitle()}"
                                    >
                                        <option
                                            value="${task.authorId().toString()}"
                                            selected
                                        >
                                            ${task.authorName()}
                                        </option>

                                        @for(TaskParticipantView member : members)
                                            @if(!member.id().equals(task.authorId()))
                                                <option
                                                    value="${member.id().toString()}"
                                                >
                                                    ${member.name()}
                                                </option>
                                            @endif
                                        @endfor
                                    </select>

                                    <span
                                        id="author-warning-${task.id()}"
                                        style="
                                            display: none;
                                            position: absolute;
                                            top: 28px;
                                            left: 0;
                                            width: 320px;
                                            padding: 8px;
                                            border: 1px solid #d0d7de;
                                            background: white;
                                            color: #9a6700;
                                            z-index: 10;
                                        "
                                    >
                                        После смены автора предыдущий автор
                                        потеряет право редактировать задачу.
                                    </span>
                                </span>
                            @else
                                <select
                                    name="authorId"
                                    data-inline-control
                                    data-original-value="${task.authorId().toString()}"
                                    onchange="showInlineSavePanel(this)"
                                    style="${task.participants().author().removedFromTeamStyle()}"
                                    title="${task.participants().author().removedFromTeamTitle()}"
                                >
                                    <option
                                        value="${task.authorId().toString()}"
                                        selected
                                    >
                                        ${task.authorName()}
                                    </option>

                                    @for(TaskParticipantView member : members)
                                        @if(!member.id().equals(task.authorId()))
                                            <option
                                                value="${member.id().toString()}"
                                            >
                                                ${member.name()}
                                            </option>
                                        @endif
                                    @endfor
                                </select>
                            @endif

                            <span
                                data-inline-actions
                                style="display: none;"
                            >
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button
                                    type="button"
                                    onclick="cancelInlineUpdate(this)"
                                >
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <div>
                    <span>Исполнитель:</span>

                    @if(!task.canUpdateTask())
                        @if(task.assigneeRemovedFromTeam())
                            <span style="color: red;" title="Пользователь был удалён из команды">
                                ${task.assigneeName()}
                            </span>
                        @else
                            <span>${task.assigneeName()}</span>
                        @endif
                    @else
                        <form
                            method="post"
                            action="/tasks/${task.id()}/assignee"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            <select
                                name="assigneeId"
                                data-inline-control
                                data-original-value="${task.assigneeId().toString()}"
                                onchange="showInlineSavePanel(this)"
                                style="${task.participants().assignee().removedFromTeamStyle()}"
                                title="${task.participants().assignee().removedFromTeamTitle()}"
                            >
                                <option
                                    value="${task.assigneeId().toString()}"
                                    selected
                                >
                                    ${task.assigneeName()}
                                </option>

                                @for(TaskParticipantView member : members)
                                    @if(!member.id().equals(task.assigneeId()))
                                        <option
                                            value="${member.id().toString()}"
                                        >
                                            ${member.name()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            <span
                                data-inline-actions
                                style="display: none;"
                            >
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button
                                    type="button"
                                    onclick="cancelInlineUpdate(this)"
                                >
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <div>
                    <span>Дедлайн:</span>

                    @if(task.deadlineOverdue())
                        <span style="color: red;">
                            ${task.deadlineText()}
                        </span>
                    @else
                        <span>
                            ${task.deadlineText()}
                        </span>
                    @endif

                    @if(task.canUpdateTask())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/deadline"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            @if(task.deadlineAt() == null)
                                <input
                                    type="date"
                                    name="deadlineDate"
                                    data-inline-control
                                    data-original-value=""
                                    onchange="showInlineSavePanel(this)"
                                >
                            @else
                                <input
                                    type="date"
                                    name="deadlineDate"
                                    value="${task.deadlineAt().toString().substring(0, 10)}"
                                    data-inline-control
                                    data-original-value="${task.deadlineAt().toString().substring(0, 10)}"
                                    onchange="showInlineSavePanel(this)"
                                >
                            @endif

                            <span
                                data-inline-actions
                                style="display: none;"
                            >
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button
                                    type="button"
                                    onclick="cancelInlineUpdate(this)"
                                >
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <!-- Изменено: категория полностью заменена тегом. -->
                <div>
                    <span>Тег:</span>

                    @if(task.canUpdateTask())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/tag"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            <select
                                name="tagId"
                                data-inline-control
                                data-original-value="${task.tagId()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                @for(TeamTag tag : tags)
                                    <option
                                        value="${tag.getId()}"
                                        selected="${tag.getId().equals(task.tagId())}"
                                    >
                                        ${tag.getName()}
                                    </option>
                                @endfor
                            </select>

                            <span
                                data-inline-actions
                                style="display: none;"
                            >
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button
                                    type="button"
                                    onclick="cancelInlineUpdate(this)"
                                >
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @else
                        <span>${task.tagName()}</span>
                    @endif
                </div>

                <div>
                    <span>Команда:</span>

                    <a href="/teams/${team.getId()}">
                        ${team.getName()}
                    </a>
                </div>

                @if(task.archived())
                    @if(task.canRestore())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/restore"
                            style="display: inline;"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            <button type="submit">
                                Вернуть из архива
                            </button>
                        </form>
                    @endif
                @else
                    @if(task.canArchive())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/archive"
                            style="display: inline;"
                        >
                            <input
                                type="hidden"
                                name="${_csrf.getParameterName()}"
                                value="${_csrf.getToken()}"
                            >

                            <input
                                type="hidden"
                                name="returnTo"
                                value="task"
                            >

                            <button type="submit">
                                Перенести в архив
                            </button>
                        </form>
                    @endif
                @endif
            </div>
        </section>

        <section class="task-description-section">
            <h2>Описание</h2>

            <div class="task-description-panel">
                @if(task.description() == null || task.description().isBlank())
                    <p>
                        Описание пока не добавлено.
                    </p>
                @else
                    <p>${task.description()}</p>
                @endif
            </div>

            @if(task.canUpdateTask())
                <button
                    class="text-action"
                    type="button"
                    onclick="document.getElementById('description-edit-form').style.display = 'block'; this.style.display = 'none'; return false;"
                >
                    Редактировать описание
                </button>

                <form
                    id="description-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    style="display: none; margin-top: 12px;"
                >
                    <input
                        type="hidden"
                        name="${_csrf.getParameterName()}"
                        value="${_csrf.getToken()}"
                    >

                    <input
                        type="hidden"
                        name="title"
                        value="${task.title()}"
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

                    @if(task.deadlineAt() != null)
                        <input
                            type="hidden"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif

                    <div>
                        <label>Описание</label>

                        <textarea
                            name="description"
                            required
                        >@if(task.description() != null)${task.description()}@endif</textarea>
                    </div>

                    <button type="submit">
                        Сохранить описание
                    </button>

                    <button
                        type="button"
                        onclick="document.getElementById('description-edit-form').style.display = 'none';"
                    >
                        Отмена
                    </button>
                </form>
            @endif
        </section>

        <section class="comments-section">
            <h2>Комментарии</h2>

            @if(comments.isEmpty())
                <p>
                    Комментариев пока нет.
                </p>
            @else
                <ul class="comment-timeline">
                    @for(CommentView comment : comments)
                        <li class="comment-item">
                            <span class="comment-avatar" aria-hidden="true">${comment.author().initial()}</span>

                            <article class="comment-card">
                                <header class="comment-header">
                                    <div>
                                        <strong>${comment.author().displayName()}</strong>
                                        <small>${comment.createdAtText()}</small>

                                        @if(comment.edited())
                                            <span class="comment-edited">Отредактировано</span>
                                        @endif
                                    </div>

                                    @if(comment.canEdit())
                                        <details class="comment-menu">
                                            <summary aria-label="Действия с комментарием">•••</summary>

                                            <div class="comment-menu-panel">
                                                <button
                                                    type="button"
                                                    onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'block'; this.closest('details').open = false;"
                                                >
                                                    Редактировать
                                                </button>

                                                <form method="post" action="/tasks/${task.id()}/comments/${comment.id()}/delete">
                                                    <input
                                                        type="hidden"
                                                        name="${_csrf.getParameterName()}"
                                                        value="${_csrf.getToken()}"
                                                    >

                                                    <button class="comment-delete-action" type="submit">Удалить</button>
                                                </form>
                                            </div>
                                        </details>
                                    @endif
                                </header>

                                <div class="comment-body">${comment.message()}</div>

                            @if(comment.canEdit())
                                <form
                                    id="comment-edit-form-${comment.id()}"
                                    class="comment-edit-form"
                                    method="post"
                                    action="/tasks/${task.id()}/comments/${comment.id()}/edit"
                                    style="display: none; margin-top: 8px;"
                                >
                                    <input
                                        type="hidden"
                                        name="${_csrf.getParameterName()}"
                                        value="${_csrf.getToken()}"
                                    >

                                    <textarea
                                        name="message"
                                        required
                                    >${comment.message()}</textarea>

                                    <button type="submit">
                                        Сохранить
                                    </button>

                                    <button
                                        type="button"
                                        onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'none';"
                                    >
                                        Отмена
                                    </button>
                                </form>
                            @endif
                            </article>
                        </li>
                    @endfor
                </ul>
            @endif

            <form
                class="comment-create-form form-card"
                method="post"
                action="/tasks/${task.id()}/comments"
            >
                <input
                    type="hidden"
                    name="${_csrf.getParameterName()}"
                    value="${_csrf.getToken()}"
                >

                <div>
                    <label>Комментарий</label>

                    <textarea
                        name="message"
                        required
                    ></textarea>
                </div>

                <button type="submit">
                    Добавить комментарий
                </button>
            </form>
        </section>
        </div>
    `
)
