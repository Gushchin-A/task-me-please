@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.Task
@import dev.gushchin.taskmanager.view.TeamTasksStats
@import java.util.List

@param Team team
@param List<Task> tasks
@param TeamTasksStats stats

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
                <span>Открыто (${stats.openCount()})</span>
                <span>В работе (${stats.inProgressCount()})</span>
                <span>Готово (${stats.doneCount()})</span>
                <span>Не актуально (${stats.notRelevantCount()})</span>
            </div>

            <div>
                <span>Сортировка:</span>
                <span>Исполнитель</span>
                <span>Автор</span>
                <span>Дедлайн</span>
                <span>Сервис</span>
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
                            <h3>${task.getTitle()}</h3>

                            <p>Статус: ${task.getStatus().name()}</p>
                            <p>Автор: ${task.getAuthorId().toString()}</p>

                            @if(task.getDeadlineAt() == null)
                                <p>Дедлайн: не указан</p>
                            @else
                                <p>Дедлайн: ${task.getDeadlineAt().toString()}</p>
                            @endif

                            <p>Категория: ${task.getCategory().name()}</p>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)