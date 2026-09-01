@import dev.gushchin.taskmanager.model.Team
@import java.util.List
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String successMessage
@param String errorMessage

@template.layout.page(
    title = "Команды",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    successMessage = successMessage,
    errorMessage = errorMessage,
    headerContext = null,
    content = @`
        <section class="workspace-page teams-page">
            <div class="teams-toolbar${teams.isEmpty() ? " teams-toolbar-empty" : ""}">
                @if(!teams.isEmpty())
                    <span class="teams-count">Команды (${teams.size()})</span>
                @endif

                <a class="button button-primary team-create-action" href="/teams/new">
                    Создать команду
                </a>
            </div>

            @if(teams.isEmpty())
                <div class="empty-state team-empty-state">
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
                        <circle cx="12" cy="7" r="3.25"></circle>
                        <circle cx="5.25" cy="8.25" r="2.25"></circle>
                        <circle cx="18.75" cy="8.25" r="2.25"></circle>
                        <path d="M6.5 20.25H3.25v-3.5a4.25 4.25 0 0 1 5.25-4.13"></path>
                        <path d="M17.5 20.25h3.25v-3.5a4.25 4.25 0 0 0-5.25-4.13"></path>
                        <path d="M7.25 20.25v-4.5a4.75 4.75 0 0 1 9.5 0v4.5Z"></path>
                    </svg>

                    <h2>Команд пока нет</h2>
                    <p>Создайте команду, чтобы распределять задачи и работать вместе</p>
                </div>
            @else
                <ul class="team-list">
                    @for(Team team : teams)
                        <li class="team-list-item">
                            <a
                                class="team-list-link"
                                href="/teams/${team.getId()}"
                                data-tooltip="Открыть команду"
                                aria-label="Открыть команду ${team.getName()}"
                            >
                                <span class="team-list-mark" aria-hidden="true">
                                    ${team.getName().substring(0, 1).toUpperCase()}
                                </span>

                                <span class="team-list-copy">
                                    <strong>${team.getName()}</strong>
                                    <small>Команда</small>
                                </span>

                                <svg
                                    class="team-list-arrow"
                                    aria-hidden="true"
                                    width="16"
                                    height="16"
                                    viewBox="0 0 16 16"
                                    fill="currentColor"
                                >
                                    <path d="M6.22 3.22a.75.75 0 0 1 1.06 0l4.25 4.25a.75.75 0 0 1 0 1.06l-4.25 4.25a.75.75 0 0 1-1.06-1.06L9.94 8 6.22 4.28a.75.75 0 0 1 0-1.06Z"></path>
                                </svg>
                            </a>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)
