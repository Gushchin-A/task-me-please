@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamInvitation
@import org.springframework.security.web.csrf.CsrfToken

@param TeamInvitation invitation
@param Team team
@param String invitedByEmail
@param CsrfToken _csrf

@template.layout.page(
    title = "Приглашение в команду",
    _csrf = _csrf,
    content = @`
        <section>
            <h1>Приглашение в команду</h1>

            <p>
                Вас пригласил ${invitedByEmail} в команду «${team.getName()}».
            </p>

            <form method="post" action="/invitations/${invitation.getToken()}/accept">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <button type="submit">
                    Принять приглашение
                </button>
            </form>

            <form method="post" action="/invitations/${invitation.getToken()}/decline">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <button type="submit">
                    Отклонить приглашение
                </button>
            </form>
        </section>
    `
)
