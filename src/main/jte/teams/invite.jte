@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.TeamInvitationView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param CsrfToken _csrf
@param boolean canInvite
@param int invitationExpirationDays
@param List<TeamInvitationView> invitations
@param String successMessage
@param String errorMessage

@template.layout.page(
    title = team.getName() + " — пригласить",
    _csrf = _csrf,
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
                        Отправить приглашение
                    </button>

                    <p>Ссылка-приглашение действует ${invitationExpirationDays} дней.</p>
                </form>
            @else
                <p>Вы можете просматривать команду, но не можете приглашать новых участников.</p>
            @endif

            @if(canInvite)
                <h2>Статус отправленных приглашений</h2>

                <table>
                    <thead>
                        <tr>
                            <th>Email получателя</th>
                            <th>Status</th>
                            <th>Ссылка приглашение</th>
                            <th>Действие</th>
                        </tr>
                    </thead>

                    <tbody>
                        @for(TeamInvitationView invitation : invitations)
                            <tr>
                                <td>${invitation.invitedEmail()}</td>
                                <td>${invitation.status()}</td>
                                <td>
                                    @if(invitation.invitationUrl() != null)
                                        <a href="${invitation.invitationUrl()}">
                                            ${invitation.invitationUrl()}
                                        </a>
                                    @else
                                        —
                                    @endif
                                </td>
                                <td>
                                    @if(invitation.canCancel())
                                        <div id="cancel-invitation-action-${invitation.id()}">
                                            <button
                                                type="button"
                                                onclick="document.getElementById('cancel-invitation-confirm-${invitation.id()}').style.display = 'inline'; document.getElementById('cancel-invitation-action-${invitation.id()}').style.display = 'none';"
                                            >
                                                Отменить приглашение
                                            </button>
                                        </div>

                                        <form
                                            id="cancel-invitation-confirm-${invitation.id()}"
                                            method="post"
                                            action="/teams/${team.getId()}/invitations/${invitation.id()}/cancel"
                                            style="display: none;"
                                        >
                                            <input
                                                type="hidden"
                                                name="${_csrf.getParameterName()}"
                                                value="${_csrf.getToken()}"
                                            >

                                            <button type="submit">
                                                Да
                                            </button>

                                            <button
                                                type="button"
                                                onclick="document.getElementById('cancel-invitation-confirm-${invitation.id()}').style.display = 'none'; document.getElementById('cancel-invitation-action-${invitation.id()}').style.display = 'inline';"
                                            >
                                                Нет
                                            </button>
                                        </form>
                                    @else
                                        —
                                    @endif
                                </td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            @endif
        </section>
    `
)
