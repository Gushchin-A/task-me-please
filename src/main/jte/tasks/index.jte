@import dev.gushchin.taskmanager.model.TaskRoleFilter
@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.MyTasksPageView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.TaskWithTeamView
@import org.springframework.security.web.csrf.CsrfToken

@param MyTasksPageView page
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Мои задачи",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    content = @`
        <section class="workspace-page tasks-page">
            <nav class="local-tabs" aria-label="Режим списка задач">
                <a
                    class="${page.activeMode() ? "local-tab-active" : ""}"
                    href="/tasks"
                    aria-current="${page.activeMode() ? "page" : null}"
                >
                    Задачи
                </a>

                <a
                    class="${page.archiveMode() ? "local-tab-active" : ""}"
                    href="/tasks/archive"
                    aria-current="${page.archiveMode() ? "page" : null}"
                >
                    Архив
                </a>
            </nav>

            <div class="task-toolbar">
                <span class="task-count">Задачи (${page.totalTasksCount()})</span>

                @if(!page.tasks().isEmpty())
                    <details class="toolbar-popover">
                    <summary class="button button-secondary toolbar-icon-button" data-tooltip="Фильтры">
                        <span aria-hidden="true">▽</span>
                        <span class="visually-hidden">Фильтры</span>
                    </summary>

                    <div class="toolbar-popover-panel">

                <a href="${page.allStatusesUrl()}">
                    @if(page.selectedStatus() == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                <a href="${page.statusUrl(TaskStatus.OPEN)}">
                    @if(page.selectedStatus() == TaskStatus.OPEN)
                        <strong>Открыто (${page.stats().openCount()})</strong>
                    @else
                        Открыто (${page.stats().openCount()})
                    @endif
                </a>

                <a href="${page.statusUrl(TaskStatus.IN_PROGRESS)}">
                    @if(page.selectedStatus() == TaskStatus.IN_PROGRESS)
                        <strong>В работе (${page.stats().inProgressCount()})</strong>
                    @else
                        В работе (${page.stats().inProgressCount()})
                    @endif
                </a>

                <a href="${page.statusUrl(TaskStatus.DONE)}">
                    @if(page.selectedStatus() == TaskStatus.DONE)
                        <strong>Готово (${page.stats().doneCount()})</strong>
                    @else
                        Готово (${page.stats().doneCount()})
                    @endif
                </a>

                <a href="${page.statusUrl(TaskStatus.NOT_RELEVANT)}">
                    @if(page.selectedStatus() == TaskStatus.NOT_RELEVANT)
                        <strong>Не актуально (${page.stats().notRelevantCount()})</strong>
                    @else
                        Не актуально (${page.stats().notRelevantCount()})
                    @endif
                </a>

                <a href="${page.roleUrl(TaskRoleFilter.AUTHOR)}">
                    @if(page.selectedRole() == TaskRoleFilter.AUTHOR)
                        <strong>Я автор (${page.authorTasksCount()})</strong>
                    @else
                        Я автор (${page.authorTasksCount()})
                    @endif
                </a>

                <a href="${page.roleUrl(TaskRoleFilter.ASSIGNEE)}">
                    @if(page.selectedRole() == TaskRoleFilter.ASSIGNEE)
                        <strong>Я исполнитель (${page.assigneeTasksCount()})</strong>
                    @else
                        Я исполнитель (${page.assigneeTasksCount()})
                    @endif
                </a>

                @if(page.selectedRole() != null)
                    <a href="${page.allRolesUrl()}">
                        Сбросить фильтр по роли
                    </a>
                @endif
                    </div>
                    </details>

                    <details class="toolbar-popover">
                    <summary class="button button-secondary" data-tooltip="Команды">Команда</summary>

                    <div class="toolbar-popover-panel">

                <a href="${page.allTeamsUrl()}">
                    @if(page.selectedTeamId() == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                @for(Team team : page.visibleTeams())
                    <a href="${page.teamUrl(team.getId())}">
                        @if(page.selectedTeamId() != null && page.selectedTeamId().equals(team.getId()))
                            <strong>${team.getName()}</strong>
                        @else
                            ${team.getName()}
                        @endif
                    </a>
                @endfor
                    </div>
                    </details>

                    <details class="toolbar-popover">
                    <summary class="button button-secondary toolbar-icon-button" data-tooltip="Сортировка">
                        <span aria-hidden="true">⇅</span>
                        <span class="visually-hidden">Сортировка</span>
                    </summary>

                    <div class="toolbar-popover-panel">

				<a href="${page.sortUrl(TaskSort.DEADLINE)}">
					@if(page.selectedSort() == TaskSort.DEADLINE)
						<strong>Дедлайн</strong>
					@else
						Дедлайн
					@endif
				</a>

				<a href="${page.sortUrl(TaskSort.TAG)}">
					@if(page.selectedSort() == TaskSort.TAG)
						<strong>Тег</strong>
					@else
						Тег
					@endif
				</a>
                    </div>
                    </details>
                @endif

                <a class="button button-primary task-create-action" href="/tasks/new">Создать задачу</a>
            </div>

            @if(page.tasks().isEmpty())
                <div class="empty-state task-empty-state">
                    <svg
                        class="empty-state-icon"
                        aria-hidden="true"
                        focusable="false"
                        viewBox="0 0 24 24"
                        width="24"
                        height="24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    >
                        <path d="M7.5 5.25H5.75A2.75 2.75 0 0 0 3 8v11.25A1.75 1.75 0 0 0 4.75 21h14.5A1.75 1.75 0 0 0 21 19.25V8a2.75 2.75 0 0 0-2.75-2.75H16.5"></path>
                        <path d="M7.5 2.75v4M16.5 2.75v4"></path>
                        <path d="M7.5 10h7M7.5 13.5h5"></path>
                        <path d="m14 17 1.75 1.75 3.25-4.5"></path>
                    </svg>
                    <h2>Задачи не найдены</h2>
                    <p>Измените фильтры или создайте новую задачу.</p>
                </div>
            @else
                <ul class="task-grid">
                    @for(TaskWithTeamView taskCard : page.tasks())
                        <li class="task-card">
                            <h3>
                                <a href="/tasks/${taskCard.task().id()}">
                                    ${taskCard.task().title()}
                                </a>
                            </h3>

                            @if(taskCard.task().archived())
                                <p>Архив</p>
                            @endif

                            <div>
                                <span>Статус:</span>

                                @if(taskCard.task().canUpdateStatus())
                                    <form
                                        method="post"
                                        action="/tasks/${taskCard.task().id()}/status"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                        <input type="hidden" name="returnTo" value="tasks">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        @if(page.selectedTeamId() != null)
                                            <input type="hidden" name="selectedTeamId" value="${page.selectedTeamId()}">
                                        @endif

                                        @if(page.selectedRole() != null)
                                            <input type="hidden" name="selectedRole" value="${page.selectedRole().name()}">
                                        @endif

                                        <select
                                            name="status"
                                            data-inline-control
                                            data-original-value="${taskCard.task().status().name()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${taskCard.task().status().name()}" selected>
                                                ${taskCard.task().status().name()}
                                            </option>

                                            @for(TaskStatus status : TaskStatus.values())
                                                @if(status != taskCard.task().status())
                                                    <option value="${status.name()}">
                                                        ${status.name()}
                                                    </option>
                                                @endif
                                            @endfor
                                        </select>

                                        <span data-inline-actions style="display: none;">
                                            <button type="submit">
                                                Сохранить
                                            </button>

                                            <button type="button" onclick="cancelInlineUpdate(this)">
                                                Отмена
                                            </button>
                                        </span>
                                    </form>
                                @else
                                    <span>${taskCard.task().status().name()}</span>
                                @endif
                            </div>

                            <div>
                                <span>Автор:</span>

                                @if(!taskCard.task().canUpdateTask())
                                    @if(taskCard.task().authorRemovedFromTeam())
                                        <span style="color: red;" title="Пользователь был удалён из команды">
                                            ${taskCard.task().authorName()}
                                        </span>
                                    @else
                                        <span>${taskCard.task().authorName()}</span>
                                    @endif
                                @else
                                    <form
                                        method="post"
                                        action="/tasks/${taskCard.task().id()}/author"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                        <input type="hidden" name="returnTo" value="tasks">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        @if(page.selectedTeamId() != null)
                                            <input type="hidden" name="selectedTeamId" value="${page.selectedTeamId()}">
                                        @endif

                                        @if(page.selectedRole() != null)
                                            <input type="hidden" name="selectedRole" value="${page.selectedRole().name()}">
                                        @endif

                                        @if(taskCard.task().showAuthorChangeWarning())
                                            <span style="position: relative; display: inline-block;">
                                                <select
                                                    name="authorId"
                                                    data-inline-control
                                                    data-original-value="${taskCard.task().authorId().toString()}"
                                                    onchange="showInlineSavePanel(this)"
                                                    onmouseenter="document.getElementById('author-warning-${taskCard.task().id()}').style.display = 'block';"
                                                    onmouseleave="document.getElementById('author-warning-${taskCard.task().id()}').style.display = 'none';"
                                                    style="${taskCard.task().participants().author().removedFromTeamStyle()}"
                                                    title="${taskCard.task().participants().author().removedFromTeamTitle()}"
                                                >
                                                    <option value="${taskCard.task().authorId().toString()}" selected>
                                                        ${taskCard.task().authorName()}
                                                    </option>

                                                    @for(TaskParticipantView member : taskCard.members())
                                                        @if(!member.id().equals(taskCard.task().authorId()))
                                                            <option value="${member.id().toString()}">
                                                                ${member.name()}
                                                            </option>
                                                        @endif
                                                    @endfor
                                                </select>

                                                <span
                                                    id="author-warning-${taskCard.task().id()}"
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
                                                    После смены автора предыдущий автор потеряет право редактировать задачу.
                                                </span>
                                            </span>
                                        @else
                                            <select
                                                name="authorId"
                                                data-inline-control
                                                data-original-value="${taskCard.task().authorId().toString()}"
                                                onchange="showInlineSavePanel(this)"
                                                style="${taskCard.task().participants().author().removedFromTeamStyle()}"
                                                title="${taskCard.task().participants().author().removedFromTeamTitle()}"
                                            >
                                                <option value="${taskCard.task().authorId().toString()}" selected>
                                                    ${taskCard.task().authorName()}
                                                </option>

                                                @for(TaskParticipantView member : taskCard.members())
                                                    @if(!member.id().equals(taskCard.task().authorId()))
                                                        <option value="${member.id().toString()}">
                                                            ${member.name()}
                                                        </option>
                                                    @endif
                                                @endfor
                                            </select>
                                        @endif

                                        <span data-inline-actions style="display: none;">
                                            <button type="submit">
                                                Сохранить
                                            </button>

                                            <button type="button" onclick="cancelInlineUpdate(this)">
                                                Отмена
                                            </button>
                                        </span>
                                    </form>
                                @endif
                            </div>

                            <div>
                                <span>Исполнитель:</span>

                                @if(!taskCard.task().canUpdateTask())
                                    @if(taskCard.task().assigneeRemovedFromTeam())
                                        <span style="color: red;" title="Пользователь был удалён из команды">
                                            ${taskCard.task().assigneeName()}
                                        </span>
                                    @else
                                        <span>${taskCard.task().assigneeName()}</span>
                                    @endif
                                @else
                                    <form
                                        method="post"
                                        action="/tasks/${taskCard.task().id()}/assignee"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                        <input type="hidden" name="returnTo" value="tasks">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        @if(page.selectedTeamId() != null)
                                            <input type="hidden" name="selectedTeamId" value="${page.selectedTeamId()}">
                                        @endif

                                        @if(page.selectedRole() != null)
                                            <input type="hidden" name="selectedRole" value="${page.selectedRole().name()}">
                                        @endif

                                        <select
                                            name="assigneeId"
                                            data-inline-control
                                            data-original-value="${taskCard.task().assigneeId().toString()}"
                                            onchange="showInlineSavePanel(this)"
                                            style="${taskCard.task().participants().assignee().removedFromTeamStyle()}"
                                            title="${taskCard.task().participants().assignee().removedFromTeamTitle()}"
                                        >
                                            <option value="${taskCard.task().assigneeId().toString()}" selected>
                                                ${taskCard.task().assigneeName()}
                                            </option>

                                            @for(TaskParticipantView member : taskCard.members())
                                                @if(!member.id().equals(taskCard.task().assigneeId()))
                                                    <option value="${member.id().toString()}">
                                                        ${member.name()}
                                                    </option>
                                                @endif
                                            @endfor
                                        </select>

                                        <span data-inline-actions style="display: none;">
                                            <button type="submit">
                                                Сохранить
                                            </button>

                                            <button type="button" onclick="cancelInlineUpdate(this)">
                                                Отмена
                                            </button>
                                        </span>
                                    </form>
                                @endif
                            </div>

                            <div>
                                <span>Дедлайн:</span>

                                @if(taskCard.task().deadlineOverdue())
                                    <span style="color: red;">
                                        ${taskCard.task().deadlineText()}
                                    </span>
                                @else
                                    <span>
                                        ${taskCard.task().deadlineText()}
                                    </span>
                                @endif

                                @if(taskCard.task().canUpdateTask())
                                    <form
                                        method="post"
                                        action="/tasks/${taskCard.task().id()}/deadline"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                        <input type="hidden" name="returnTo" value="tasks">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        @if(page.selectedTeamId() != null)
                                            <input type="hidden" name="selectedTeamId" value="${page.selectedTeamId()}">
                                        @endif

                                        @if(page.selectedRole() != null)
                                            <input type="hidden" name="selectedRole" value="${page.selectedRole().name()}">
                                        @endif

                                        @if(taskCard.task().deadlineAt() == null)
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
                                                value="${taskCard.task().deadlineAt().toString().substring(0, 10)}"
                                                data-inline-control
                                                data-original-value="${taskCard.task().deadlineAt().toString().substring(0, 10)}"
                                                onchange="showInlineSavePanel(this)"
                                            >
                                        @endif

                                        <span data-inline-actions style="display: none;">
                                            <button type="submit">
                                                Сохранить
                                            </button>

                                            <button type="button" onclick="cancelInlineUpdate(this)">
                                                Отмена
                                            </button>
                                        </span>
                                    </form>
                                @endif
                            </div>

                            <div>
                                <span>Тег:</span>

                                @if(taskCard.task().canUpdateTask())
                                    <form
                                        method="post"
                                        action="/tasks/${taskCard.task().id()}/tag"
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
                                            value="tasks"
                                        >

                                        @if(page.selectedStatus() != null)
                                            <input
                                                type="hidden"
                                                name="selectedStatus"
                                                value="${page.selectedStatus().name()}"
                                            >
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input
                                                type="hidden"
                                                name="selectedSort"
                                                value="${page.selectedSort().name()}"
                                            >
                                        @endif

                                        @if(page.selectedTeamId() != null)
                                            <input
                                                type="hidden"
                                                name="selectedTeamId"
                                                value="${page.selectedTeamId()}"
                                            >
                                        @endif

                                        @if(page.selectedRole() != null)
                                            <input
                                                type="hidden"
                                                name="selectedRole"
                                                value="${page.selectedRole().name()}"
                                            >
                                        @endif

                                        <select
                                            name="tagId"
                                            data-inline-control
                                            data-original-value="${taskCard.task().tagId()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            @for(TeamTag tag : taskCard.tags())
                                                <option
                                                    value="${tag.getId()}"
                                                    selected="${tag.getId().equals(taskCard.task().tagId())}"
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
                                    <span>${taskCard.task().tagName()}</span>
                                @endif
                            </div>

                            <div>
                                <span>Команда:</span>

                                <a href="/teams/${taskCard.teamId()}">
                                    ${taskCard.teamName()}
                                </a>
                            </div>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)
