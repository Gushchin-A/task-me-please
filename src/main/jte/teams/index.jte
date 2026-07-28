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
        <section class="workspace-page teams-page">
            <div class="page-actions">
                <a class="button button-primary" href="/teams/new">
                    Создать команду
                </a>
            </div>

            @if(teams.isEmpty())
                <div class="empty-state">
                    <h2>Команд пока нет</h2>
                    <p>Создайте команду, чтобы распределять задачи и работать вместе.</p>
                    <a class="button button-primary" href="/teams/new">Создать команду</a>
                </div>
            @else
                <ul class="team-grid">
                    @for(Team team : teams)
                        <li class="team-card">
                            <a
                                class="team-card-link"
                                href="/teams/${team.getId()}"
                                data-tooltip="Открыть команду"
                                aria-label="Открыть команду ${team.getName()}"
                            >
                                <span class="team-card-mark" aria-hidden="true">
                                    ${team.getName().substring(0, 1).toUpperCase()}
                                </span>

                                <span>
                                    <strong>${team.getName()}</strong>
                                </span>

                                <span class="team-card-arrow" aria-hidden="true">→</span>
                            </a>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)
