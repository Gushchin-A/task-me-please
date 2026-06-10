@import java.util.List
@import dev.gushchin.taskmanager.model.Team

@param List<Team> teams

@template.layout.page(
    title = "Главная",
    content = @`
        <section>
            <h2>Задачи</h2>

            <p>Все (0) Исполнитель (0) Автор (0) Открыто (0) В работе (0) Готово (0)</p>

            <a href="/tasks">Посмотреть все задачи</a>
            <a href="/tasks/new">Создать</a>
        </section>

        <section>
            <h2>Команды</h2>

            <a href="/teams/new">Создать команду</a>

            @if(teams.isEmpty())
                <p>У вас нет команд</p>
            @else
                <ul>
                    @for(var team : teams)
                        <li>
                            <a href="/teams/${team.getId()}">${team.getName()}</a>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)