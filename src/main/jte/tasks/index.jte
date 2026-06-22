@import dev.gushchin.taskmanager.model.TaskCategory
@import dev.gushchin.taskmanager.model.TaskRoleFilter
@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.User
@import dev.gushchin.taskmanager.view.MyTasksPageView
@import dev.gushchin.taskmanager.view.TaskWithTeamView
@import org.springframework.security.web.csrf.CsrfToken

@param MyTasksPageView page
@param CsrfToken _csrf

@template.layout.page(
    title = "Мои задачи",
    content = @`
        <section>
            <h1>Мои задачи</h1>

            <div>
                <a href="/tasks">
                    @if(page.activeMode())
                        <strong>Задачи</strong>
                    @else
                        Задачи
                    @endif
                </a>

                <a href="/tasks/archive">
                    @if(page.archiveMode())
                        <strong>Архив</strong>
                    @else
                        Архив
                    @endif
                </a>
            </div>

            <p>Здесь отображаются все ваши задачи.</p>

            <div>
                <strong>Задачи (${page.totalTasksCount()})</strong>
            </div>

            <div>
                <span>Фильтр:</span>

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

            <div>
                <span>Команда:</span>

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

            <div>
                <span>Сортировка:</span>

                <a href="${page.sortUrl(TaskSort.DEADLINE)}">
                    @if(page.selectedSort() == TaskSort.DEADLINE)
                        <strong>Дедлайн</strong>
                    @else
                        Дедлайн
                    @endif
                </a>

                <a href="${page.sortUrl(TaskSort.CATEGORY)}">
                    @if(page.selectedSort() == TaskSort.CATEGORY)
                        <strong>Сервис</strong>
                    @else
                        Сервис
                    @endif
                </a>
            </div>

            @if(page.tasks().isEmpty())
                <p>Задачи по выбранным фильтрам отсутствуют.</p>
            @else
                <ul style="list-style: none; padding-left: 0; margin-left: 0;">
                    @for(TaskWithTeamView taskCard : page.tasks())
                        <li
                            style="
                                background: #f6f8fa;
                                border: 1px solid #d0d7de;
                                padding: 16px;
                                margin-bottom: 12px;
                                max-width: 720px;
                            "
                        >
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
                            </div>

                            <div>
                                <span>Автор:</span>

                                @if(taskCard.members().size() == 1)
                                    <span>${taskCard.task().authorName()}</span>
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

                                        <select
                                            name="authorId"
                                            data-inline-control
                                            data-original-value="${taskCard.task().authorId().toString()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${taskCard.task().authorId().toString()}" selected>
                                                ${taskCard.task().authorName()}
                                            </option>

                                            @for(User member : taskCard.members())
                                                @if(!member.getId().equals(taskCard.task().authorId()))
                                                    <option value="${member.getId().toString()}">
                                                        ${member.getName()}
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
                                <span>Исполнитель:</span>

                                @if(taskCard.members().size() == 1)
                                    <span>${taskCard.task().assigneeName()}</span>
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
                                        >
                                            <option value="${taskCard.task().assigneeId().toString()}" selected>
                                                ${taskCard.task().assigneeName()}
                                            </option>

                                            @for(User member : taskCard.members())
                                                @if(!member.getId().equals(taskCard.task().assigneeId()))
                                                    <option value="${member.getId().toString()}">
                                                        ${member.getName()}
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
                            </div>

                            <div>
                                <span>Категория:</span>

                                <form
                                    method="post"
                                    action="/tasks/${taskCard.task().id()}/category"
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
                                        name="category"
                                        data-inline-control
                                        data-original-value="${taskCard.task().category().name()}"
                                        onchange="showInlineSavePanel(this)"
                                    >
                                        <option value="${taskCard.task().category().name()}" selected>
                                            ${taskCard.task().category().name()}
                                        </option>

                                        @for(TaskCategory category : TaskCategory.values())
                                            @if(category != taskCard.task().category())
                                                <option value="${category.name()}">
                                                    ${category.name()}
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