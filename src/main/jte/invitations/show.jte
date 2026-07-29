@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamInvitation
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param TeamInvitation invitation
@param Team team
@param String invitedByEmail
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Приглашение в команду",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = null,
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
