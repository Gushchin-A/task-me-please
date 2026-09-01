@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.TaskRoleFilter
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.MyTasksPageView
@import dev.gushchin.taskmanager.view.TaskParticipantView

@param MyTasksPageView page

<div class="task-toolbar">
    <span class="task-count">Задачи (${page.totalTasksCount()})</span>

    <div class="toolbar-popover task-filter-popover" data-toolbar-select>
        <button class="primer-select-trigger task-toolbar-trigger" type="button" aria-haspopup="dialog" aria-expanded="false">
            <span class="primer-select-value">Фильтр</span>
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
        </button>

        <div class="primer-select-panel task-filter-panel my-tasks-filter-panel" hidden>
            <div class="primer-select-heading task-select-heading">
                <span>Выберите фильтры</span>
                <button class="task-select-close" type="button" aria-label="Закрыть фильтры" data-toolbar-select-close>
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path></svg>
                </button>
            </div>

            <div class="primer-select-options task-filter-options">
                <div class="task-filter-row">
                    <span>Статус</span>
                    <div class="primer-select task-filter-select" data-filter-select>
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden><div class="primer-select-options">
                            @for(TaskParticipantView member : page.filterMembers())
                                <a class="primer-select-option" href="${page.authorUrl(member.id())}" role="option" aria-selected="${page.selectedAuthorId() != null && page.selectedAuthorId().equals(member.id())}">${member.name()}</a>
                            @endfor
                        </div></div>
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden><div class="primer-select-options">
                            @for(TeamTag tag : page.tags())
                                <a class="primer-select-option" href="${page.tagUrl(tag.getId())}" role="option" aria-selected="${page.selectedTagId() != null && page.selectedTagId().equals(tag.getId())}">${tag.getName()}</a>
                            @endfor
                        </div></div>
                    </div>
                </div>

                <div class="task-filter-row">
                    <span>Команда</span>
                    <div class="primer-select task-filter-select" data-filter-select>
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                            <span class="primer-select-value">
                                @for(Team team : page.teams())
                                    @if(page.selectedTeamId() != null && page.selectedTeamId().equals(team.getId()))
                                        ${team.getName()}
                                    @endif
                                @endfor
                            </span>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden><div class="primer-select-options">
                            @for(Team team : page.teams())
                                <a class="primer-select-option" href="${page.teamUrl(team.getId())}" role="option" aria-selected="${page.selectedTeamId() != null && page.selectedTeamId().equals(team.getId())}">${team.getName()}</a>
                            @endfor
                        </div></div>
                    </div>
                </div>

                <div class="task-filter-row">
                    <span>Моя роль</span>
                    <div class="primer-select task-filter-select" data-filter-select>
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                            <span class="primer-select-value">
                                @if(page.selectedRole() == TaskRoleFilter.AUTHOR)
                                    Автор
                                @elseif(page.selectedRole() == TaskRoleFilter.ASSIGNEE)
                                    Исполнитель
                                @endif
                            </span>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden><div class="primer-select-options">
                            <a class="primer-select-option" href="${page.roleUrl(TaskRoleFilter.AUTHOR)}" role="option" aria-selected="${page.selectedRole() == TaskRoleFilter.AUTHOR}">Автор</a>
                            <a class="primer-select-option" href="${page.roleUrl(TaskRoleFilter.ASSIGNEE)}" role="option" aria-selected="${page.selectedRole() == TaskRoleFilter.ASSIGNEE}">Исполнитель</a>
                        </div></div>
                    </div>
                </div>

                <div class="task-filter-space" aria-hidden="true"></div>
                <div class="task-filter-actions"><a class="button button-secondary task-filter-reset" href="${page.clearFiltersUrl()}">Сбросить</a></div>
            </div>
        </div>
    </div>

    <div class="toolbar-popover task-sort-popover" data-toolbar-select>
        <button class="primer-select-trigger task-toolbar-trigger" type="button" aria-haspopup="menu" aria-expanded="false">
            <span class="primer-select-value">Сортировка</span>
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
        </button>
        <div class="primer-select-panel task-sort-panel" hidden>
            <div class="primer-select-heading task-select-heading">
                <span>Сортировать задачи</span>
                <button class="task-select-close" type="button" aria-label="Закрыть сортировку" data-toolbar-select-close>
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path></svg>
                </button>
            </div>
            <div class="primer-select-options" role="menu">
                @for(TaskSort taskSort : TaskSort.values())
                    <a class="primer-select-option" href="${page.sortUrl(taskSort)}" role="menuitem" aria-current="${page.selectedSort() == taskSort || page.selectedSort() == null && taskSort == TaskSort.NEWEST ? "true" : null}">
                        <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                        <span>${taskSort.getDisplayName()}</span>
                    </a>
                @endfor
            </div>
        </div>
    </div>

    <a class="button button-primary task-create-action" href="/tasks/new">Создать задачу</a>
</div>
