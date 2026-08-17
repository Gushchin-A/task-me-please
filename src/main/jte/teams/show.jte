@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.TaskView
@import dev.gushchin.taskmanager.view.TeamPageView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TeamPageView page
@param List<Team> navigationTeams
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = page.team().getName(),
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    headerContext = @`
        @template.components.teamSwitcher(team = page.team(), navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(
            team = page.team(),
            activeTab = page.archiveMode() ? "archive" : "tasks"
        )

        <section class="workspace-page team-details-page">
            @if(!page.hasNoTasks())
                <div class="task-toolbar">
                <div class="toolbar-popover task-filter-popover" data-toolbar-select>
                    <button
                        class="primer-select-trigger task-toolbar-trigger"
                        type="button"
                        aria-haspopup="dialog"
                        aria-expanded="false"
                    >
                        <span class="primer-select-value">Фильтр</span>
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

                    <div class="primer-select-panel task-filter-panel" hidden>
                        <div class="primer-select-heading task-select-heading">
                            <span>Выберите фильтры</span>
                            <button
                                class="task-select-close"
                                type="button"
                                aria-label="Закрыть фильтры"
                                data-toolbar-select-close
                            >
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path>
                                </svg>
                            </button>
                        </div>

                        <div class="primer-select-options task-filter-options">
                            <div class="task-filter-row">
                                <span>Статус</span>
                                <div class="primer-select task-filter-select" data-filter-select>
                                    <button
                                        class="primer-select-trigger"
                                        type="button"
                                        aria-haspopup="listbox"
                                        aria-expanded="false"
                                    >
                                        <span class="primer-select-value">
                                            @if(page.selectedStatus() == TaskStatus.OPEN)
                                                Открыто
                                            @elseif(page.selectedStatus() == TaskStatus.IN_PROGRESS)
                                                В работе
                                            @elseif(page.selectedStatus() == TaskStatus.DONE)
                                                Готово
                                            @elseif(page.selectedStatus() == TaskStatus.NOT_RELEVANT)
                                                Не актуально
                                            @endif
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden>
                                        <div class="primer-select-options">
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.OPEN)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.OPEN}">Открыто (${page.stats().openCount()})</a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.IN_PROGRESS)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.IN_PROGRESS}">В работе (${page.stats().inProgressCount()})</a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.DONE)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.DONE}">Готово (${page.stats().doneCount()})</a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.NOT_RELEVANT)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.NOT_RELEVANT}">Не актуально (${page.stats().notRelevantCount()})</a>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Исполнитель</span>
                                <div class="primer-select task-filter-select" data-filter-select>
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                                        <span class="primer-select-value">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                @if(page.selectedAssigneeId() != null && page.selectedAssigneeId().equals(member.id()))
                                                    ${member.name()}
                                                @endif
                                            @endfor
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden>
                                        <div class="primer-select-options">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                <a
                                                    class="primer-select-option"
                                                    href="${page.assigneeUrl(member.id())}"
                                                    role="option"
                                                    aria-selected="${page.selectedAssigneeId() != null && page.selectedAssigneeId().equals(member.id())}"
                                                    style="${member.removedFromTeamStyle()}"
                                                    title="${member.removedFromTeamTitle()}"
                                                >${member.name()}</a>
                                            @endfor
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Автор</span>
                                <div class="primer-select task-filter-select" data-filter-select>
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                                        <span class="primer-select-value">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                @if(page.selectedAuthorId() != null && page.selectedAuthorId().equals(member.id()))
                                                    ${member.name()}
                                                @endif
                                            @endfor
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden>
                                        <div class="primer-select-options">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                <a
                                                    class="primer-select-option"
                                                    href="${page.authorUrl(member.id())}"
                                                    role="option"
                                                    aria-selected="${page.selectedAuthorId() != null && page.selectedAuthorId().equals(member.id())}"
                                                    style="${member.removedFromTeamStyle()}"
                                                    title="${member.removedFromTeamTitle()}"
                                                >${member.name()}</a>
                                            @endfor
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Тег</span>
                                <div class="primer-select task-filter-select" data-filter-select>
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                                        <span class="primer-select-value">
                                            @for(TeamTag tag : page.tags())
                                                @if(page.selectedTagId() != null && page.selectedTagId().equals(tag.getId()))
                                                    ${tag.getName()}
                                                @endif
                                            @endfor
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden>
                                        <div class="primer-select-options">
                                            @for(TeamTag tag : page.tags())
                                                <a class="primer-select-option" href="${page.tagUrl(tag.getId())}" role="option" aria-selected="${page.selectedTagId() != null && page.selectedTagId().equals(tag.getId())}">${tag.getName()}</a>
                                            @endfor
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-space" aria-hidden="true"></div>

                            <div class="task-filter-actions">
                                <a class="button button-secondary task-filter-reset" href="${page.clearFiltersUrl()}">Сбросить</a>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="toolbar-popover task-sort-popover" data-toolbar-select>
                    <button
                        class="primer-select-trigger task-toolbar-trigger"
                        type="button"
                        aria-haspopup="menu"
                        aria-expanded="false"
                    >
                        <span class="primer-select-value">Сортировка</span>
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

                    <div class="primer-select-panel task-sort-panel" hidden>
                        <div class="primer-select-heading task-select-heading">
                            <span>Сортировать задачи</span>
                            <button
                                class="task-select-close"
                                type="button"
                                aria-label="Закрыть сортировку"
                                data-toolbar-select-close
                            >
                                <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                    <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path>
                                </svg>
                            </button>
                        </div>
                        <div class="primer-select-options" role="menu">
                            <a
                                class="primer-select-option"
                                href="${page.sortUrl(TaskSort.DEADLINE)}"
                                role="menuitem"
                                aria-current="${page.selectedSort() == TaskSort.DEADLINE ? "true" : null}"
                            >
                                <span class="primer-select-check" aria-hidden="true">
                                    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                        <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                    </svg>
                                </span>
                                <span>По дедлайну</span>
                            </a>

                            <a
                                class="primer-select-option"
                                href="${page.sortUrl(TaskSort.TAG)}"
                                role="menuitem"
                                aria-current="${page.selectedSort() == TaskSort.TAG ? "true" : null}"
                            >
                                <span class="primer-select-check" aria-hidden="true">
                                    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                        <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                    </svg>
                                </span>
                                <span>По тегу</span>
                            </a>

                            <a
                                class="primer-select-option"
                                href="${page.defaultSortUrl()}"
                                role="menuitem"
                                aria-current="${page.selectedSort() == null ? "true" : null}"
                                data-tooltip="Сначала новые задачи, затем старые"
                            >
                                <span class="primer-select-check" aria-hidden="true">
                                    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                        <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                    </svg>
                                </span>
                                <span>По умолчанию</span>
                            </a>
                        </div>
                    </div>
                </div>

                <a
                    class="button button-primary task-create-action"
                    href="/tasks/new?teamId=${page.team().getId()}"
                >
                    Создать задачу
                </a>
                </div>
            @else
                <div class="page-actions tasks-onboarding-actions">
                    <a
                        class="button button-primary task-create-action"
                        href="/tasks/new?teamId=${page.team().getId()}"
                    >
                        Создать задачу
                    </a>
                </div>
            @endif

            @if(page.tasks().isEmpty())
                @if(page.archiveMode() && page.totalTasksCount() == 0)
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

                        <h2>В архиве пока что пусто</h2>
                        <p>Сюда вы сможете перенести выполненные, неактуальные или удаленные задачи</p>
                    </div>
                @elseif(page.hasNoTasks())
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

                        <h2>В команде пока нет задач</h2>
                        <p>Создайте первую задачу и назначьте ответственного.</p>
                    </div>
                @elseif(page.totalTasksCount() > 0 && page.hasSelectedFilters())
                    <div class="empty-state">
                        <h2>Задачи не найдены</h2>
                        <p>Измените фильтры, чтобы увидеть другие задачи команды.</p>
                    </div>
                @else
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

                        <h2>В команде пока нет задач</h2>
                        <p>Создайте первую задачу и назначьте ответственного.</p>
                    </div>
                @endif
            @else
                <ul class="task-grid">
                    @for(TaskView task : page.tasks())
                        @template.components.taskCard(
                            task = task,
                            teamId = null,
                            teamName = null,
                            members = page.members(),
                            tags = page.tags(),
                            returnTo = page.archiveMode() ? "teamArchive" : "team",
                            _csrf = _csrf
                        )
                    @endfor
                </ul>
            @endif

        </section>
    `
)
