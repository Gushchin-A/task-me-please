@import dev.gushchin.taskmanager.model.Team
@import java.util.List
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Команды",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    content = @`
        <section>
            <h1>Команды</h1>

            <p>Здесь отображаются команды, в которых вы состоите.</p>

            @if(teams.isEmpty())
                <p>У вас пока нет команд.</p>
            @else
                <ul>
                    @for(Team team : teams)
                        <li>
                            <a href="/teams/${team.getId()}">
                                ${team.getName()}
                            </a>
                        </li>
                    @endfor
                </ul>
            @endif

            <a href="/teams/new">
                Создать команду
            </a>
        </section>
    `
)
