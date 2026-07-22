@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.view.TaskParticipantView
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

                @for(TaskParticipantView member : page.filterMembers())
                    <a
                        href="${page.assigneeUrl(member.id())}"
                        style="${member.removedFromTeamStyle()}"
                        title="${member.removedFromTeamTitle()}"
                    >
                        @if(page.selectedAssigneeId() != null && page.selectedAssigneeId().equals(member.id()))
                            <strong>${member.name()}</strong>
                        @else
                            ${member.name()}
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

                @for(TaskParticipantView member : page.filterMembers())
                    <a
                        href="${page.authorUrl(member.id())}"
                        style="${member.removedFromTeamStyle()}"
                        title="${member.removedFromTeamTitle()}"
                    >
                        @if(page.selectedAuthorId() != null && page.selectedAuthorId().equals(member.id()))
                            <strong>${member.name()}</strong>
                        @else
                            ${member.name()}
                        @endif
                    </a>
                @endfor

                <span>По тегу:</span>

                <a href="${page.allTagsUrl()}">
                    @if(page.selectedTagId() == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                @for(TeamTag tag : page.tags())
                    <a href="${page.tagUrl(tag.getId())}">
                        @if(page.selectedTagId() != null && page.selectedTagId().equals(tag.getId()))
                            <strong>${tag.getName()}</strong>
                        @else
                            ${tag.getName()}
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

                <a href="${page.sortUrl(TaskSort.TAG)}">
                    @if(page.selectedSort() == TaskSort.TAG)
                        <strong>Тег</strong>
                    @else
                        Тег
                    @endif
                </a>
            </div>

            <p>${page.taskCountText()}</p>

            @if(page.tasks().isEmpty())
                @if(page.totalTasksCount() > 0 && page.hasSelectedFilters())
                    <p>Задачи по выбранным фильтрам отсутствуют.</p>
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
                                        <input
                                            type="hidden"
                                            name="${_csrf.getParameterName()}"
                                            value="${_csrf.getToken()}"
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

                                                    @for(TaskParticipantView member : page.members())
                                                        @if(!member.id().equals(task.authorId()))
                                                            <option value="${member.id().toString()}">
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

                                                @for(TaskParticipantView member : page.members())
                                                    @if(!member.id().equals(task.authorId()))
                                                        <option value="${member.id().toString()}">
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

                                            @for(TaskParticipantView member : page.members())
                                                @if(!member.id().equals(task.assigneeId()))
                                                    <option value="${member.id().toString()}">
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

                                        <select
                                            name="tagId"
                                            data-inline-control
                                            data-original-value="${task.tagId()}"
                                            onchange="showInlineSavePanel(this)"
                                        >
                                            @for(TeamTag tag : page.tags())
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
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)
