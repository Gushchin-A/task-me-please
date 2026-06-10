@import java.util.List
@import dev.gushchin.taskmanager.model.Team

@param List<Team> teams

@template.layout.page(
    title = "Команды",
    content = @`
        <section>
            <h1>Команды</h1>

            <p>Здесь отображаются команды, в которых вы состоите.</p>

            <a href="/teams/new">Создать команду</a>

            @if(teams.isEmpty())
                <p>У вас пока нет команд.</p>
            @else
                <ul>
                    @for(var team : teams)
                        <li>
                            <a href="/teams/${team.getId()}">
                                ${team.getName()}
                            </a>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)