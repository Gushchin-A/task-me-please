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
        <button class="primer-select-trigger task-toolbar-trigger" type="button" aria-haspopup="dialog" aria-expanded="${page.openFilter() != null}">
            <span class="primer-select-value">Фильтр</span>
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
        </button>

        <div class="primer-select-panel task-filter-panel my-tasks-filter-panel" hidden="${page.openFilter() == null}">
            <div class="primer-select-heading task-select-heading">
                <span>Выберите фильтры</span>
                <button class="task-select-close" type="button" aria-label="Закрыть фильтры" data-toolbar-select-close>
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path></svg>
                </button>
            </div>

            <div class="primer-select-options task-filter-options">
                <div class="task-filter-row">
                    <span>Статус</span>
                    <div class="primer-select task-filter-select" data-filter-select data-filter-key="status">
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"status".equals(page.openFilter())}">
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden="${!"authors".equals(page.openFilter())}"><div class="primer-select-options">
                            @for(TaskParticipantView member : page.filterMembers())
                                <a class="primer-select-option" href="${page.authorUrl(member.id())}" role="option" aria-selected="${page.selectedAuthorIds().contains(member.id())}" data-filter-option>
                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                    <span data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                </a>
                            @endfor
                        </div></div>
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden="${!"assignees".equals(page.openFilter())}"><div class="primer-select-options">
                            @for(TaskParticipantView member : page.filterMembers())
                                <a class="primer-select-option" href="${page.assigneeUrl(member.id())}" role="option" aria-selected="${page.selectedAssigneeIds().contains(member.id())}" data-filter-option>
                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                    <span data-tooltip="${member.filterDisplayNameTooltip()}">${member.filterDisplayName()}</span>
                                </a>
                            @endfor
                        </div></div>
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
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden="${!"tags".equals(page.openFilter())}"><div class="primer-select-options">
                            @for(TeamTag tag : page.tags())
                                <a class="primer-select-option" href="${page.tagUrl(tag.getId())}" role="option" aria-selected="${page.selectedTagIds().contains(tag.getId())}" data-filter-option>
                                    <span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>
                                    <span>${tag.getName()}</span>
                                </a>
                            @endfor
                        </div></div>
                    </div>
                </div>

                <div class="task-filter-row">
                    <span>Команда</span>
                    <div class="primer-select task-filter-select" data-filter-select data-filter-key="team">
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"team".equals(page.openFilter())}">
                            <span class="primer-select-value task-filter-selected-values">
                                @for(Team team : page.teams())
                                    @if(page.selectedTeamId() != null && page.selectedTeamId().equals(team.getId()))
                                        <span class="task-filter-chip">${team.getName()}</span>
                                    @endif
                                @endfor
                            </span>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden="${!"team".equals(page.openFilter())}"><div class="primer-select-options">
                            @for(Team team : page.teams())
                                <a class="primer-select-option" href="${page.teamUrl(team.getId())}" role="option" aria-selected="${page.selectedTeamId() != null && page.selectedTeamId().equals(team.getId())}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>${team.getName()}</span></a>
                            @endfor
                        </div></div>
                    </div>
                </div>

                <div class="task-filter-row">
                    <span>Моя роль</span>
                    <div class="primer-select task-filter-select" data-filter-select data-filter-key="role">
                        <button class="primer-select-trigger" type="button" aria-haspopup="listbox" aria-expanded="${"role".equals(page.openFilter())}">
                            <span class="primer-select-value task-filter-selected-values">
                                @if(page.selectedRole() == TaskRoleFilter.AUTHOR)
                                    <span class="task-filter-chip">Автор</span>
                                @elseif(page.selectedRole() == TaskRoleFilter.ASSIGNEE)
                                    <span class="task-filter-chip">Исполнитель</span>
                                @endif
                            </span>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel" role="listbox" hidden="${!"role".equals(page.openFilter())}"><div class="primer-select-options">
                            <a class="primer-select-option" href="${page.roleUrl(TaskRoleFilter.AUTHOR)}" role="option" aria-selected="${page.selectedRole() == TaskRoleFilter.AUTHOR}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>Автор</span></a>
                            <a class="primer-select-option" href="${page.roleUrl(TaskRoleFilter.ASSIGNEE)}" role="option" aria-selected="${page.selectedRole() == TaskRoleFilter.ASSIGNEE}" data-filter-option><span class="primer-select-check" aria-hidden="true">✓</span><span>Исполнитель</span></a>
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
