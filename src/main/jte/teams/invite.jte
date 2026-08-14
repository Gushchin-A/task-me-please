@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.TeamInvitationView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param List<Team> navigationTeams
@param CsrfToken _csrf
@param boolean canInvite
@param int invitationExpirationDays
@param List<TeamInvitationView> invitations
@param String successMessage
@param String errorMessage
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — пригласить",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    successMessage = successMessage,
    errorMessage = errorMessage,
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = "invite"
        )

        <section class="workspace-page team-invite-page">
            @if(canInvite)
                <form
                    class="invite-form"
                    method="post"
                    action="/teams/${team.getId()}/members"
                    data-submit-loading
                >
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                    <div class="form-field">
                        <label for="invitation-email">Email пользователя</label>

                        <input
                            id="invitation-email"
                            type="email"
                            name="email"
                            required
                        >
                    </div>

                    <button
                        class="button button-primary"
                        type="submit"
                        data-loading-text="Отправляем приглашение…"
                    >
                        Отправить приглашение
                    </button>

                    <p class="form-help">Ссылка-приглашение действует ${invitationExpirationDays} дней.</p>
                </form>
            @else
                <p>Вы можете просматривать команду, но не можете приглашать новых участников.</p>
            @endif

            @if(canInvite)
                <h2>Статус отправленных приглашений</h2>

                <div class="table-card">
                <table class="data-table invitation-table">
                    <thead>
                        <tr>
                            <th>Email получателя</th>
                            <th>Status</th>
                            <th>Ссылка приглашение</th>
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

                                    @if(invitation.canCancel())
                                        <form
                                            method="post"
                                            action="/teams/${team.getId()}/invitations/${invitation.id()}/resend"
                                            style="display: inline;"
                                            data-submit-loading
                                        >
                                            <input
                                                type="hidden"
                                                name="${_csrf.getParameterName()}"
                                                value="${_csrf.getToken()}"
                                            >

                                            <button
                                                class="button button-secondary"
                                                type="submit"
                                                data-loading-text="Отправляем письмо…"
                                            >
                                                Отправить письмо повторно
                                            </button>
                                        </form>

                                        <div
                                            id="cancel-invitation-action-${invitation.id()}"
                                            style="display: inline;"
                                        >
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
                                    @endif
                                </td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
                </div>
            @endif
        </section>
    `
)
