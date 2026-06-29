@import dev.gushchin.taskmanager.model.TaskCategory
@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.User
@import dev.gushchin.taskmanager.view.TaskView
@import dev.gushchin.taskmanager.view.TeamPageView
@import org.springframework.security.web.csrf.CsrfToken

@param TeamPageView page
@param CsrfToken _csrf

@template.layout.page(
    title = page.team().getName(),
    content = @`
        <section>
            <h1>${page.team().getName()}</h1>

            <div>
                <a href="/teams/${page.team().getId()}">
                    @if(page.activeMode())
                        <strong>Задачи (${page.totalTasksCount()})</strong>
                    @else
                        Задачи
                    @endif
                </a>

                <a href="/teams/${page.team().getId()}/members">
                    Участники (${page.membersCount()})
                </a>

                <a href="/teams/${page.team().getId()}/archive">
                    @if(page.archiveMode())
                        <strong>Архив</strong>
                    @else
                        Архив
                    @endif
                </a>

                @if(page.canInvite())
                    <a href="/teams/${page.team().getId()}/invite">
                        Пригласить в команду
                    </a>
                @else
                    <a
                        href="#"
                        onclick="showFlashMessage('Только owner команды может приглашать новых участников. Вы можете пока только просматривать команду.'); return false;"
                    >
                        Пригласить в команду
                    </a>
                @endif
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

                <span>По исполнителю:</span>

                <a href="${page.allAssigneesUrl()}">
                    @if(page.selectedAssigneeId() == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                @for(User member : page.members())
                    <a href="${page.assigneeUrl(member.getId())}">
                        @if(page.selectedAssigneeId() != null && page.selectedAssigneeId().equals(member.getId()))
                            <strong>${member.getName()}</strong>
                        @else
                            ${member.getName()}
                        @endif
                    </a>
                @endfor

                <span>По автору:</span>

                <a href="${page.allAuthorsUrl()}">
                    @if(page.selectedAuthorId() == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                @for(User member : page.members())
                    <a href="${page.authorUrl(member.getId())}">
                        @if(page.selectedAuthorId() != null && page.selectedAuthorId().equals(member.getId()))
                            <strong>${member.getName()}</strong>
                        @else
                            ${member.getName()}
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

            <p>${page.taskCountText()}</p>

            @if(page.tasks().isEmpty())
                @if(page.totalTasksCount() > 0 && page.selectedStatus() != null)
                    <p>Задачи по выбранному фильтру отсутствуют.</p>
                @else
                    <p>Пока в этой команде нет задач.</p>

                    <a href="/tasks/new?teamId=${page.team().getId()}">
                        Создать задачу
                    </a>
                @endif
            @else
                <ul style="list-style: none; padding-left: 0; margin-left: 0;">
                    @for(TaskView task : page.tasks())
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
                                <a href="/tasks/${task.id()}">
                                    ${task.title()}
                                </a>
                            </h3>

                            @if(task.archived())
                                <p>Архив</p>
                            @endif

                            <div>
                                <span>Статус:</span>

                                @if(task.canUpdateStatus())
                                    <form
                                        method="post"
                                        action="/tasks/${task.id()}/status"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        <select
                                            name="status"
                                            data-inline-control
                                            data-original-value="${task.status().name()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${task.status().name()}" selected>
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
                                    <span>${task.status().name()}</span>
                                @endif
                            </div>

                            <div>
                                <span>Автор:</span>

                                @if(!task.canUpdateTask() || page.members().size() == 1)
                                    <span>${task.authorName()}</span>
                                @else
                                    <form
                                        method="post"
                                        action="/tasks/${task.id()}/author"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        <select
                                            name="authorId"
                                            data-inline-control
                                            data-original-value="${task.authorId().toString()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${task.authorId().toString()}" selected>
                                                ${task.authorName()}
                                            </option>

                                            @for(User member : page.members())
                                                @if(!member.getId().equals(task.authorId()))
                                                    <option value="${member.getId().toString()}">
                                                        ${member.getName()}
                                                    </option>
                                                @endif
                                            @endfor
                                        </select>

                                        @if(task.showAuthorChangeWarning())
                                            <p style="color: #9a6700; margin: 4px 0 0 0;">
                                                После смены автора предыдущий автор потеряет право редактировать задачу.
                                            </p>
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

                                @if(!task.canUpdateTask() || page.members().size() == 1)
                                    <span>${task.assigneeName()}</span>
                                @else
                                    <form
                                        method="post"
                                        action="/tasks/${task.id()}/assignee"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        <select
                                            name="assigneeId"
                                            data-inline-control
                                            data-original-value="${task.assigneeId().toString()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${task.assigneeId().toString()}" selected>
                                                ${task.assigneeName()}
                                            </option>

                                            @for(User member : page.members())
                                                @if(!member.getId().equals(task.assigneeId()))
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
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

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
                                <span>Категория:</span>

                                @if(task.canUpdateTask())
                                    <form
                                        method="post"
                                        action="/tasks/${task.id()}/category"
                                        style="display: inline;"
                                        data-inline-update-form="true"
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                                        @if(page.selectedStatus() != null)
                                            <input type="hidden" name="selectedStatus" value="${page.selectedStatus().name()}">
                                        @endif

                                        @if(page.selectedSort() != null)
                                            <input type="hidden" name="selectedSort" value="${page.selectedSort().name()}">
                                        @endif

                                        <select
                                            name="category"
                                            data-inline-control
                                            data-original-value="${task.category().name()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            <option value="${task.category().name()}" selected>
                                                ${task.category().name()}
                                            </option>

                                            @for(TaskCategory category : TaskCategory.values())
                                                @if(category != task.category())
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
                                @else
                                    <span>${task.category().name()}</span>
                                @endif
                            </div>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)