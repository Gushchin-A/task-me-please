@import dev.gushchin.taskmanager.model.TaskSort
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.TaskView
@import dev.gushchin.taskmanager.view.TeamTasksStats
@import java.util.List

@param Team team
@param List<TaskView> tasks
@param TeamTasksStats stats
@param TaskStatus selectedStatus
@param TaskSort selectedSort

@template.layout.page(
    title = team.getName(),
    content = @`
        <section>
            @if(tasks.isEmpty())
                <h1>${team.getName()}</h1>
            @else
                <h1>
                    ${team.getName()}
                    <a href="/tasks/new?teamId=${team.getId()}">
                        Создать задачу
                    </a>
                </h1>
            @endif

            <p>Всего задач: ${tasks.size()}</p>

            <div>
                <a href="/teams/${team.getId()}@if(selectedSort != null)?sort=${selectedSort.name()}@endif">
                    @if(selectedStatus == null)
                        <strong>Все</strong>
                    @else
                        Все
                    @endif
                </a>

                <a href="/teams/${team.getId()}?status=OPEN@if(selectedSort != null)&sort=${selectedSort.name()}@endif">
                    @if(selectedStatus == TaskStatus.OPEN)
                        <strong>Открыто (${stats.openCount()})</strong>
                    @else
                        Открыто (${stats.openCount()})
                    @endif
                </a>

                <a href="/teams/${team.getId()}?status=IN_PROGRESS@if(selectedSort != null)&sort=${selectedSort.name()}@endif">
                    @if(selectedStatus == TaskStatus.IN_PROGRESS)
                        <strong>В работе (${stats.inProgressCount()})</strong>
                    @else
                        В работе (${stats.inProgressCount()})
                    @endif
                </a>

                <a href="/teams/${team.getId()}?status=DONE@if(selectedSort != null)&sort=${selectedSort.name()}@endif">
                    @if(selectedStatus == TaskStatus.DONE)
                        <strong>Готово (${stats.doneCount()})</strong>
                    @else
                        Готово (${stats.doneCount()})
                    @endif
                </a>

                <a href="/teams/${team.getId()}?status=NOT_RELEVANT@if(selectedSort != null)&sort=${selectedSort.name()}@endif">
                    @if(selectedStatus == TaskStatus.NOT_RELEVANT)
                        <strong>Не актуально (${stats.notRelevantCount()})</strong>
                    @else
                        Не актуально (${stats.notRelevantCount()})
                    @endif
                </a>
            </div>

            <div>
                <span>Сортировка:</span>

                <a href="/teams/${team.getId()}?sort=DEADLINE@if(selectedStatus != null)&status=${selectedStatus.name()}@endif">
                    @if(selectedSort == TaskSort.DEADLINE)
                        <strong>Дедлайн</strong>
                    @else
                        Дедлайн
                    @endif
                </a>

                <a href="/teams/${team.getId()}?sort=CATEGORY@if(selectedStatus != null)&status=${selectedStatus.name()}@endif">
                    @if(selectedSort == TaskSort.CATEGORY)
                        <strong>Сервис</strong>
                    @else
                        Сервис
                    @endif
                </a>
            </div>

            @if(tasks.isEmpty())
                <p>Пока в этой команде нет задач.</p>

                <a href="/tasks/new?teamId=${team.getId()}">
                    Создать задачу
                </a>
            @else
                <ul>
                    @for(var task : tasks)
                        <li>
                            <h3>${task.title()}</h3>

                            <p>Статус: ${task.status().name()}</p>
                            <p>Автор: ${task.authorName()}</p>

                            @if(task.deadlineAt() == null)
                                <p>Дедлайн: не указан</p>
                            @else
                                <p>Дедлайн: ${task.deadlineAt().toString()}</p>
                            @endif

                            <p>Категория: ${task.category().name()}</p>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)