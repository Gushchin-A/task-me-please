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
@param String successMessage

@template.layout.page(
    title = page.team().getName(),
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    successMessage = successMessage,
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
                <span class="task-count">Задачи (${page.totalTasksCount()})</span>

                <div class="toolbar-popover task-filter-popover" data-toolbar-select>
                    <button
                        class="primer-select-trigger task-toolbar-trigger"
                        type="button"
                        aria-haspopup="dialog"
                        aria-expanded="${page.openFilter() != null}"
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

                    <div class="primer-select-panel task-filter-panel" hidden="${page.openFilter() == null}">
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
                                <div class="primer-select task-filter-select" data-filter-select data-filter-key="status">
                                    <button
                                        class="primer-select-trigger"
                                        type="button"
                                        aria-haspopup="listbox"
                                        aria-expanded="${"status".equals(page.openFilter())}"
                                    >
                                        <span class="primer-select-value task-filter-selected-values">
                                            @if(page.selectedStatus() == TaskStatus.OPEN)
                                                <span class="task-filter-chip">Открыто</span>
                                            @elseif(page.selectedStatus() == TaskStatus.IN_PROGRESS)
                                                <span class="task-filter-chip">В работе</span>
                                            @elseif(page.selectedStatus() == TaskStatus.DONE)
                                                <span class="task-filter-chip">Готово</span>
                                            @elseif(page.selectedStatus() == TaskStatus.NOT_RELEVANT)
                                                <span class="task-filter-chip">Не актуально</span>
                                            @endif
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden="${!"status".equals(page.openFilter())}">
                                        <div class="primer-select-options">
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.OPEN)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.OPEN}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>Открыто (${page.stats().openCount()})</span></a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.IN_PROGRESS)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.IN_PROGRESS}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>В работе (${page.stats().inProgressCount()})</span></a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.DONE)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.DONE}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>Готово (${page.stats().doneCount()})</span></a>
                                            <a class="primer-select-option" href="${page.statusUrl(TaskStatus.NOT_RELEVANT)}" role="option" aria-selected="${page.selectedStatus() == TaskStatus.NOT_RELEVANT}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>Не актуально (${page.stats().notRelevantCount()})</span></a>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Исполнитель</span>
                                <div class="primer-select task-filter-select" data-filter-select data-filter-key="assignees">
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"assignees".equals(page.openFilter())}">
                                        <span class="primer-select-value task-filter-selected-values" data-filter-selected-values>
                                            @for(TaskParticipantView member : page.filterMembers())
                                                @if(page.selectedAssigneeIds().contains(member.id()))
                                                    <span class="task-filter-chip" data-filter-chip data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                                @endif
                                            @endfor
                                            <span class="task-filter-chip task-filter-overflow" data-filter-overflow hidden></span>
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden="${!"assignees".equals(page.openFilter())}">
                                        <div class="primer-select-options">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                <a
                                                    class="primer-select-option"
                                                    href="${page.assigneeUrl(member.id())}"
                                                    role="option"
                                                    aria-selected="${page.selectedAssigneeIds().contains(member.id())}"
                                                    data-filter-option
                                                    style="${member.removedFromTeamStyle()}"
                                                    title="${member.removedFromTeamTitle()}"
                                                >
                                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                                    <span data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                                </a>
                                            @endfor
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Автор</span>
                                <div class="primer-select task-filter-select" data-filter-select data-filter-key="authors">
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"authors".equals(page.openFilter())}">
                                        <span class="primer-select-value task-filter-selected-values" data-filter-selected-values>
                                            @for(TaskParticipantView member : page.filterMembers())
                                                @if(page.selectedAuthorIds().contains(member.id()))
                                                    <span class="task-filter-chip" data-filter-chip data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                                @endif
                                            @endfor
                                            <span class="task-filter-chip task-filter-overflow" data-filter-overflow hidden></span>
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden="${!"authors".equals(page.openFilter())}">
                                        <div class="primer-select-options">
                                            @for(TaskParticipantView member : page.filterMembers())
                                                <a
                                                    class="primer-select-option"
                                                    href="${page.authorUrl(member.id())}"
                                                    role="option"
                                                    aria-selected="${page.selectedAuthorIds().contains(member.id())}"
                                                    data-filter-option
                                                    style="${member.removedFromTeamStyle()}"
                                                    title="${member.removedFromTeamTitle()}"
                                                >
                                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                                    <span data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                                </a>
                                            @endfor
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="task-filter-row">
                                <span>Тег</span>
                                <div class="primer-select task-filter-select" data-filter-select data-filter-key="tags">
                                    <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"tags".equals(page.openFilter())}">
                                        <span class="primer-select-value task-filter-selected-values" data-filter-selected-values>
                                            @for(TeamTag tag : page.tags())
                                                @if(page.selectedTagIds().contains(tag.getId()))
                                                    <span class="task-filter-chip" data-filter-chip>${tag.getName()}</span>
                                                @endif
                                            @endfor
                                            <span class="task-filter-chip task-filter-overflow" data-filter-overflow hidden></span>
                                        </span>
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path>
                                        </svg>
                                    </button>
                                    <div class="primer-select-panel" role="listbox" hidden="${!"tags".equals(page.openFilter())}">
                                        <div class="primer-select-options">
                                            @for(TeamTag tag : page.tags())
                                                <a class="primer-select-option" href="${page.tagUrl(tag.getId())}" role="option" aria-selected="${page.selectedTagIds().contains(tag.getId())}" data-filter-option>
                                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                                    <span>${tag.getName()}</span>
                                                </a>
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
                            @for(TaskSort taskSort : TaskSort.values())
                                <a
                                    class="primer-select-option"
                                    href="${page.sortUrl(taskSort)}"
                                    role="menuitem"
                                    aria-current="${page.selectedSort() == taskSort
                                        || page.selectedSort() == null && taskSort == TaskSort.NEWEST
                                        ? "true"
                                        : null}"
                                >
                                    <span class="primer-select-check" aria-hidden="true">
                                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                                        </svg>
                                    </span>
                                    <span>${taskSort.getDisplayName()}</span>
                                </a>
                            @endfor
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

                        @if(page.limitedTaskVisibility())
                            <h2>В этой команде у вас пока нет задач</h2>
                            <p>Дождитесь, пока вам поставят задачу, или создайте свою и назначьте ответственного.</p>
                        @else
                            <h2>В команде пока нет задач</h2>
                            <p>Создайте первую задачу и назначьте ответственного.</p>
                        @endif
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

                        @if(page.limitedTaskVisibility())
                            <h2>В этой команде у вас пока нет задач</h2>
                            <p>Дождитесь, пока вам поставят задачу, или создайте свою и назначьте ответственного.</p>
                        @else
                            <h2>В команде пока нет задач</h2>
                            <p>Создайте первую задачу и назначьте ответственного.</p>
                        @endif
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
