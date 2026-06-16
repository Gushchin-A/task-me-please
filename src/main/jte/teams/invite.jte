@import dev.gushchin.taskmanager.model.Team
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param CsrfToken _csrf
@param boolean canInvite
@param String successMessage
@param String errorMessage

@template.layout.page(
    title = team.getName() + " — пригласить",
    content = @`
        <section>
            <h1>${team.getName()}</h1>

            <div>
                <a href="/teams/${team.getId()}">
                    Задачи
                </a>

                <a href="/teams/${team.getId()}/members">
                    Участники
                </a>

                <a href="/teams/${team.getId()}/invite">
                    <strong>Пригласить в команду</strong>
                </a>
            </div>

            @if(successMessage != null)
                <div class="flash-message">
                    ${successMessage}
                </div>
            @endif

            @if(errorMessage != null)
                <div class="flash-message">
                    ${errorMessage}
                </div>
            @endif

            <h2>Добавить участника</h2>

            @if(canInvite)
                <form method="post" action="/teams/${team.getId()}/members">
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                    <div>
                        <label>Email пользователя</label>

                        <input
                            type="email"
                            name="email"
                            required
                        >
                    </div>

                    <button type="submit">
                        Добавить в команду
                    </button>
                </form>
            @else
                <p>Вы можете просматривать команду, но не можете приглашать новых участников.</p>
            @endif
        </section>
    `
)