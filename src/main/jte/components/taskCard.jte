@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.TaskView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TaskView task
@param Long teamId
@param String teamName
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param String returnTo
@param CsrfToken _csrf

<li class="task-card task-card-status-${task.statusCssClass()}">
    <div class="task-card-header">
        <span class="task-card-status">${task.statusText()}</span>

        <details class="task-card-actions">
            <summary class="task-card-menu" aria-label="Действия с задачей «${task.title()}»">
                <svg aria-hidden="true" viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
                    <path d="M2 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path>
                </svg>
            </summary>

            <div class="task-card-actions-panel">
                <a href="/tasks/${task.id()}">Открыть задачу</a>
                <button
                    class="task-card-action-option"
                    type="button"
                    data-task-link-copy
                    data-task-url="/tasks/${task.id()}"
                >
                    Скопировать ссылку
                </button>

                @if((!task.archived() && (task.canUpdateTask() || task.canArchive()))
                        || (task.archived() && task.canRestore()))
                    <div class="task-card-actions-divider" role="separator"></div>
                @endif

                @if(!task.archived() && task.canUpdateTask())
                    <button class="task-card-action-option" type="button" data-task-edit-open>Изменить задачу</button>
                @endif

                @if(!task.archived() && task.canArchive())
                    <form method="post" action="/tasks/${task.id()}/archive">
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <input type="hidden" name="returnTo" value="${returnTo}">

                        @if(task.status() == TaskStatus.DONE)
                            <button class="task-card-action-option" type="submit">Перенести в архив</button>
                        @else
                            <span data-tooltip="Задача будет перемещена в архив">
                                <button class="task-card-action-option task-card-delete-action" type="submit">
                                    Удалить задачу
                                </button>
                            </span>
                        @endif
                    </form>
                @endif

                @if(task.archived() && task.canRestore())
                    <form method="post" action="/tasks/${task.id()}/restore">
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <input type="hidden" name="returnTo" value="${returnTo}">
                        <button class="task-card-action-option" type="submit">Вернуть из архива</button>
                    </form>
                @endif
            </div>
        </details>
    </div>

    <div class="task-card-body">
        <h3><a href="/tasks/${task.id()}">${task.title()}</a></h3>

        <div class="task-card-labels">
            <span class="task-card-tag">${task.tagName()}</span>
            <span class="task-card-deadline ${!task.archived() && (task.deadlineToday() || task.deadlineOverdue()) ? "task-card-deadline-urgent" : ""}">
                ${task.deadlineText()}
            </span>
        </div>

        <dl class="task-card-people">
            <div>
                <dt>Автор</dt>
                <dd class="${task.authorRemovedFromTeam() ? "task-card-former-member" : ""}">${task.authorName()}</dd>
            </div>
            <div>
                <dt>Исполнитель</dt>
                <dd class="${task.assigneeRemovedFromTeam() ? "task-card-former-member" : ""}">${task.assigneeName()}</dd>
            </div>
        </dl>

        <footer class="task-card-footer">
            <time datetime="${task.updatedAt() == null ? "" : task.updatedAt().toString()}">${task.updatedAtText()}</time>

            @if(teamId != null && teamName != null)
                <a href="/teams/${teamId}">${teamName}</a>
            @endif
        </footer>
    </div>

    @if(!task.archived() && task.canUpdateTask())
        <dialog class="settings-dialog task-edit-dialog" data-task-edit-dialog>
            <form
                class="settings-dialog-form task-edit-form"
                method="post"
                action="/tasks/${task.id()}/edit"
                data-task-edit-form
                data-submit-loading
            >
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                <input type="hidden" name="returnTo" value="${returnTo}">

                <header class="settings-dialog-header">
                    <h2>Изменение задачи</h2>
                    <button class="task-edit-dialog-close" type="button" aria-label="Закрыть" data-task-edit-close>×</button>
                </header>

                <div class="settings-dialog-body task-edit-dialog-body">
                    <div class="form-field task-create-field task-title-field">
                        <label for="task-title-${task.id()}">Название задачи</label>
                        <input
                            id="task-title-${task.id()}"
                            type="text"
                            name="title"
                            value="${task.title()}"
                            maxlength="100"
                            required
                            aria-describedby="task-title-count-${task.id()}"
                        >

                        <p class="form-help task-title-count" id="task-title-count-${task.id()}">
                            <span data-task-title-length>${task.title().length()}</span> / 100 символов
                        </p>
                    </div>

                    <div class="form-field task-create-field task-status-field">
                        <label id="task-status-label-${task.id()}">Статус</label>
                        <input id="task-status-${task.id()}" type="hidden" name="status" value="${task.status().name()}">

                        <div class="primer-select" data-task-edit-select>
                            <button
                                class="primer-select-trigger"
                                type="button"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                                aria-labelledby="task-status-label-${task.id()} task-status-value-${task.id()}"
                            >
                                <span class="primer-select-value" id="task-status-value-${task.id()}">
                                    ${task.statusText()}
                                </span>
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                </svg>
                            </button>

                            <div class="primer-select-panel" hidden>
                                <div class="primer-select-heading">Выберите статус</div>
                                <div class="primer-select-options" role="listbox" aria-labelledby="task-status-label-${task.id()}">
                            @for(TaskStatus status : TaskStatus.values())
                                        <button
                                            class="primer-select-option"
                                            type="button"
                                            role="option"
                                            aria-selected="${status == task.status()}"
                                            data-value="${status.name()}"
                                            data-label="${TaskView.statusText(status)}"
                                        >
                                            <span class="primer-select-check" aria-hidden="true">
                                                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                    <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                </svg>
                                            </span>
                                            <span class="primer-select-option-copy">${TaskView.statusText(status)}</span>
                                        </button>
                            @endfor
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-field task-create-field task-assignee-field">
                        <label id="task-assignee-label-${task.id()}">Исполнитель</label>
                        <input id="task-assignee-${task.id()}" type="hidden" name="assigneeId" value="${task.assigneeId().toString()}">

                        <div class="primer-select" data-task-edit-select>
                            <button
                                class="primer-select-trigger"
                                type="button"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                                aria-labelledby="task-assignee-label-${task.id()} task-assignee-value-${task.id()}"
                            >
                                <span class="participant-avatar" data-selected-avatar>
                                    ${task.participants().assignee().initial()}
                                </span>
                                <span
                                    class="primer-select-value"
                                    id="task-assignee-value-${task.id()}"
                                    title="${task.participants().assignee().displayName()}"
                                >
                                    ${task.participants().assignee().shortDisplayName()}
                                </span>
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                </svg>
                            </button>

                            <div class="primer-select-panel" hidden>
                                <div class="primer-select-heading">Выберите исполнителя</div>
                                <div class="primer-select-options" role="listbox" aria-labelledby="task-assignee-label-${task.id()}">
                            @for(TaskParticipantView member : members)
                                        <button
                                            class="primer-select-option"
                                            type="button"
                                            role="option"
                                            aria-selected="${member.id().equals(task.assigneeId())}"
                                            data-value="${member.id().toString()}"
                                            data-label="${member.shortDisplayName()}"
                                            data-full-label="${member.displayName()}"
                                            data-avatar="${member.initial()}"
                                        >
                                            <span class="primer-select-check" aria-hidden="true">
                                                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                    <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                </svg>
                                            </span>
                                            <span class="participant-avatar">${member.initial()}</span>
                                            <span class="primer-select-option-copy">${member.displayName()}</span>
                                        </button>
                            @endfor
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-field task-create-field task-deadline-field">
                        <label for="task-deadline-${task.id()}">Дедлайн</label>
                        <input
                            id="task-deadline-${task.id()}"
                            type="date"
                            name="deadlineDate"
                            value="${task.deadlineAt() == null ? "" : task.deadlineAt().toString().substring(0, 10)}"
                        >
                    </div>

                    <div class="form-field task-create-field task-tag-field">
                        <label id="task-tag-label-${task.id()}">Тег</label>
                        <input id="task-tag-${task.id()}" type="hidden" name="tagId" value="${task.tagId()}">

                        <div class="primer-select" data-task-edit-select>
                            <button
                                class="primer-select-trigger"
                                type="button"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                                aria-labelledby="task-tag-label-${task.id()} task-tag-value-${task.id()}"
                            >
                                <span class="primer-select-value" id="task-tag-value-${task.id()}">${task.tagName()}</span>
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                </svg>
                            </button>

                            <div class="primer-select-panel" hidden>
                                <div class="primer-select-heading">Выберите тег</div>
                                <div class="primer-select-options" role="listbox" aria-labelledby="task-tag-label-${task.id()}">
                            @for(TeamTag tag : tags)
                                        <button
                                            class="primer-select-option"
                                            type="button"
                                            role="option"
                                            aria-selected="${tag.getId().equals(task.tagId())}"
                                            data-value="${tag.getId()}"
                                            data-label="${tag.getName()}"
                                            data-full-label="${tag.getName()}"
                                        >
                                            <span class="primer-select-check" aria-hidden="true">
                                                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                    <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                                </svg>
                                            </span>
                                            <span class="primer-select-option-copy">${tag.getName()}</span>
                                        </button>
                            @endfor
                                </div>
                            </div>
                        </div>
                    </div>

                    <p class="task-edit-description-help">
                        Для редактирования описания откройте
                        <a href="/tasks/${task.id()}">страницу задачи</a>.
                    </p>
                </div>

                <footer class="settings-dialog-footer task-edit-dialog-footer">
                    <button class="button button-secondary" type="button" data-task-edit-cancel>Отменить</button>
                    <button
                        class="button button-primary"
                        type="submit"
                        disabled
                        data-task-edit-submit
                        data-loading-text="Обработка..."
                    >
                        Изменить
                    </button>
                </footer>
            </form>
        </dialog>
    @endif
</li>
