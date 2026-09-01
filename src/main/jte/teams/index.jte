@import dev.gushchin.taskmanager.view.TeamListItemView
@import java.util.List
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param List<TeamListItemView> teams
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
                    @for(TeamListItemView item : teams)
                        <li class="team-list-item">
                            <a class="team-list-name" href="/teams/${item.team().getId()}">${item.team().getName()}</a>
                            <span class="team-list-meta">
                                ${item.owner() ? "Всего задач: " : "Актуальные задачи: "}${item.taskCount()}
                            </span>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    `
)
