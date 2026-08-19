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
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = "tasks"
        )

        <div class="task-detail-layout" data-task-detail>
        <section class="task-detail-main">
            <a class="team-create-back-link" href="/teams/${team.getId()}">
                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M7.78 12.53a.75.75 0 0 1-1.06 0l-4-4a.75.75 0 0 1 0-1.06l4-4a.75.75 0 0 1 1.06 1.06L5.06 7.25h7.69a.75.75 0 0 1 0 1.5H5.06l2.72 2.72a.75.75 0 0 1 0 1.06Z"></path>
                </svg>
                Вернуться назад
            </a>

            <div class="task-title-row">
                <h1>${task.title()}</h1>

                @if(task.canUpdateTask())
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

            @if(task.canUpdateTask())
                <form
                    class="task-title-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    hidden
                    data-task-title-edit-form
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

                    <label for="task-detail-title-${task.id()}">Название задачи</label>
                    <input id="task-detail-title-${task.id()}" type="text" name="title" value="${task.title()}" maxlength="100" required>

                    <div class="task-detail-edit-actions">
                        <button class="button button-secondary" type="button" data-task-title-edit-cancel>Отменить</button>
                        <button class="button button-primary" type="submit">Сохранить</button>
                    </div>
                </form>
            @endif

            <div class="task-detail-summary">
                <span class="task-detail-status task-detail-status-${task.statusCssClass()}">${task.statusText()}</span>
                <span><strong>${task.authorName()}</strong> создал задачу ${task.createdAtText()}</span>
            </div>

            @if(task.archived())
                <span class="task-detail-archive-label">Архив</span>
            @endif
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
                <span class="task-parameter-value task-detail-status task-detail-status-${task.statusCssClass()}" data-task-parameter-value>${task.statusText()}</span>
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

            <div class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}">
                <span class="task-parameter-heading">Дедлайн</span>
                <span class="task-parameter-value" data-task-parameter-value>${task.deadlineText()}</span>
                <form method="post" action="/tasks/${task.id()}/deadline" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <input type="date" name="deadlineDate" value="${task.deadlineAt() == null ? "" : task.deadlineAt().toString().substring(0, 10)}" data-original-value="${task.deadlineAt() == null ? "" : task.deadlineAt().toString().substring(0, 10)}" disabled="${!task.canUpdateTask()}">
                </form>
            </div>

            <div class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}">
                <span class="task-parameter-heading">Автор</span>
                <span class="task-parameter-value task-parameter-person ${task.authorRemovedFromTeam() ? "task-card-former-member" : ""}" title="${task.participants().author().removedFromTeamTitle()}" data-task-parameter-value>
                    <span class="task-parameter-avatar" aria-hidden="true">${task.participants().author().initial()}</span>
                    <span>${task.authorName()}</span>
                </span>
                <form method="post" action="/tasks/${task.id()}/author" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select name="authorId" data-original-value="${task.authorId().toString()}" disabled="${!task.canUpdateTask()}">
                        <option value="${task.authorId().toString()}" selected>${task.authorName()}</option>
                        @for(TaskParticipantView member : members)
                            @if(!member.id().equals(task.authorId()))
                                <option value="${member.id().toString()}">${member.name()}</option>
                            @endif
                        @endfor
                    </select>
                </form>
            </div>

            <div class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}">
                <span class="task-parameter-heading">Исполнитель</span>
                <span class="task-parameter-value task-parameter-person ${task.assigneeRemovedFromTeam() ? "task-card-former-member" : ""}" title="${task.participants().assignee().removedFromTeamTitle()}" data-task-parameter-value>
                    <span class="task-parameter-avatar" aria-hidden="true">${task.participants().assignee().initial()}</span>
                    <span>${task.assigneeName()}</span>
                </span>
                <form method="post" action="/tasks/${task.id()}/assignee" hidden data-task-parameter-form>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="returnTo" value="task">
                    <select name="assigneeId" data-original-value="${task.assigneeId().toString()}" disabled="${!task.canUpdateTask()}">
                        <option value="${task.assigneeId().toString()}" selected>${task.assigneeName()}</option>
                        @for(TaskParticipantView member : members)
                            @if(!member.id().equals(task.assigneeId()))
                                <option value="${member.id().toString()}">${member.name()}</option>
                            @endif
                        @endfor
                    </select>
                </form>
            </div>

            <div class="task-parameter-row ${!task.canUpdateTask() ? "task-parameter-read-only" : ""}">
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

            <div class="task-parameters-actions" hidden data-task-parameters-actions>
                <button class="button button-secondary" type="button" data-task-parameters-cancel>Отменить</button>
                <button class="button button-primary" type="button" disabled data-task-parameters-save>Сохранить</button>
            </div>

            @if(task.archived())
                @if(task.canRestore())
                    <form class="task-parameters-state-action" method="post" action="/tasks/${task.id()}/restore">
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <input type="hidden" name="returnTo" value="task">
                        <button class="button button-secondary" type="submit">Вернуть из архива</button>
                    </form>
                @endif
            @else
                @if(task.canArchive())
                    <form class="task-parameters-state-action" method="post" action="/tasks/${task.id()}/archive">
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <input type="hidden" name="returnTo" value="task">
                        <button class="button button-secondary" type="submit">Перенести в архив</button>
                    </form>
                @endif
            @endif
        </aside>

        <section class="task-conversation" aria-label="Обсуждение задачи">
            <article class="comment-item task-description-item">
                <span class="comment-avatar" aria-hidden="true">${task.participants().author().initial()}</span>

                <div class="comment-card">
                    <header class="comment-header">
                        <div>
                            <strong>${task.authorName()}</strong>
                            <small>добавил описание задачи · ${task.updatedAtText()}</small>
                        </div>

                        @if(task.canUpdateTask())
                            <button class="comment-header-action" type="button" aria-label="Изменить описание" data-tooltip="Изменить описание" data-description-edit-open>
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="M11.013 1.427a1.75 1.75 0 0 1 2.474 0l1.086 1.086a1.75 1.75 0 0 1 0 2.474l-8.5 8.5a.75.75 0 0 1-.33.194l-3.5 1a.75.75 0 0 1-.927-.927l1-3.5a.75.75 0 0 1 .194-.33Zm1.414 1.06a.25.25 0 0 0-.354 0L3.7 10.86l-.62 2.17 2.17-.62 8.373-8.373a.25.25 0 0 0 0-.354Z"></path>
                                </svg>
                            </button>
                        @endif
                    </header>

                    <div class="comment-body" data-description-view>
                        @if(task.description() == null || task.description().isBlank())
                            Описание пока не добавлено.
                        @else
                            ${task.description()}
                        @endif
                    </div>

                    @if(task.canUpdateTask())
                        <form class="comment-edit-form task-description-edit-form" method="post" action="/tasks/${task.id()}/edit" hidden data-description-edit-form>
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                            <input type="hidden" name="title" value="${task.title()}">
                            <input type="hidden" name="tagId" value="${task.tagId()}">
                            <input type="hidden" name="assigneeId" value="${task.assigneeId().toString()}">

                            @if(task.deadlineAt() != null)
                                <input type="hidden" name="deadlineDate" value="${task.deadlineAt().toString().substring(0, 10)}">
                            @endif

                            <textarea name="description" required>@if(task.description() != null)${task.description()}@endif</textarea>
                            <div class="comment-edit-actions">
                                <button class="button button-secondary" type="button" data-description-edit-cancel>Отменить</button>
                                <button class="button button-primary" type="submit">Сохранить</button>
                            </div>
                        </form>
                    @endif
                </div>
            </article>

            @for(CommentView comment : comments)
                <article class="comment-item">
                    <span class="comment-avatar" aria-hidden="true">${comment.author().initial()}</span>

                    <div class="comment-card">
                        <header class="comment-header">
                            <div>
                                <strong>${comment.author().displayName()}</strong>
                                <small>прокомментировал · ${comment.createdAtText()}</small>

                                @if(comment.edited())
                                    <span class="comment-edited">Отредактировано</span>
                                @endif
                            </div>

                            @if(comment.canEdit())
                                <details class="comment-menu">
                                    <summary aria-label="Действия с комментарием">•••</summary>

                                    <div class="comment-menu-panel">
                                        <button type="button" onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'block'; this.closest('details').open = false;">Редактировать</button>

                                        <form method="post" action="/tasks/${task.id()}/comments/${comment.id()}/delete">
                                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                            <button class="comment-delete-action" type="submit">Удалить</button>
                                        </form>
                                    </div>
                                </details>
                            @endif
                        </header>

                        <div class="comment-body">${comment.message()}</div>

                        @if(comment.canEdit())
                            <form id="comment-edit-form-${comment.id()}" class="comment-edit-form" method="post" action="/tasks/${task.id()}/comments/${comment.id()}/edit" style="display: none;">
                                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                <textarea name="message" required>${comment.message()}</textarea>
                                <div class="comment-edit-actions">
                                    <button class="button button-secondary" type="button" onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'none';">Отменить</button>
                                    <button class="button button-primary" type="submit">Сохранить</button>
                                </div>
                            </form>
                        @endif
                    </div>
                </article>
            @endfor

            <form class="comment-create-form" method="post" action="/tasks/${task.id()}/comments" data-submit-loading>
                <span class="comment-avatar" aria-hidden="true">${currentUser.initial()}</span>
                <div class="comment-create-content">
                    <label for="new-comment-${task.id()}">Добавить комментарий</label>
                    <textarea id="new-comment-${task.id()}" name="message" placeholder="Оставьте комментарий" required></textarea>
                    <div class="comment-create-actions">
                        <button class="button button-primary" type="submit" data-loading-text="Добавление...">Добавить комментарий</button>
                    </div>
                </div>
            </form>
        </section>

        </div>
    `
)
