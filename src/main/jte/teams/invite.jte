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
    title = team.getName() + " — приглашения",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    successMessage = successMessage,
    errorMessage = errorMessage,
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(team = team, activeTab = "settings")

        <section class="workspace-page team-settings-page">
            @template.components.teamSettingsSidebar(team = team, activeSection = "invitations", owner = canInvite)

            <div class="team-settings-content">
                <header class="team-create-header team-settings-header">
                    <h1>Приглашения</h1>
                </header>

                @if(canInvite)
                    <form
                        class="team-settings-form team-invitation-form"
                        method="post"
                        action="/teams/${team.getId()}/members"
                        data-submit-loading
                    >
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        <div class="form-field">
                            <label for="invitation-email">Email пользователя</label>
                            <div class="field-with-action">
                                <input
                                    id="invitation-email"
                                    type="email"
                                    name="email"
                                    maxlength="255"
                                    required
                                    autocomplete="email"
                                >
                                <button class="button button-secondary" type="submit" data-loading-text="Отправляем...">
                                    Отправить приглашение
                                </button>
                            </div>
                            <p class="form-help">Ссылка действует ${invitationExpirationDays} дней</p>
                        </div>
                    </form>

                    <section class="team-invitation-history" aria-labelledby="invitation-history-title">
                        <h2 id="invitation-history-title">История приглашений</h2>

                        @if(invitations.isEmpty())
                            <p class="team-invitation-empty">Приглашений пока нет</p>
                        @else
                            <div class="team-invitation-list">
                                @for(TeamInvitationView invitation : invitations)
                                    <div class="team-invitation-row">
                                        <span class="team-invitation-email" title="${invitation.invitedEmail()}">${invitation.invitedEmail()}</span>
                                        <span class="invitation-status invitation-status-${invitation.statusCssClass()}">${invitation.statusText()}</span>
                                        <time>${invitation.createdAtText()}</time>

                                        <details class="task-card-actions team-invitation-actions">
                                            <summary class="task-card-menu" aria-label="Действия с приглашением ${invitation.invitedEmail()}">
                                                <svg aria-hidden="true" viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
                                                    <path d="M2 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path>
                                                </svg>
                                            </summary>
                                            <div class="task-card-actions-panel">
                                                @if(invitation.invitationUrl() != null)
                                                    <button class="task-card-action-option" type="button" data-invitation-copy data-invitation-url="${invitation.invitationUrl()}">
                                                        Скопировать ссылку
                                                    </button>
                                                @else
                                                    <button class="task-card-action-option" type="button" data-invitation-copy data-invitation-email="${invitation.invitedEmail()}">
                                                        Скопировать email
                                                    </button>
                                                @endif

                                                @if(invitation.canCancel())
                                                    <form method="post" action="/teams/${team.getId()}/invitations/${invitation.id()}/resend" data-submit-loading>
                                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                        <button class="task-card-action-option" type="submit" data-loading-text="Отправляем...">Отправить повторно</button>
                                                    </form>
                                                    <div class="task-card-actions-divider" role="separator"></div>
                                                    <form method="post" action="/teams/${team.getId()}/invitations/${invitation.id()}/cancel">
                                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                        <button class="task-card-action-option task-card-delete-action" type="submit">Отменить приглашение</button>
                                                    </form>
                                                @endif
                                            </div>
                                        </details>
                                    </div>
                                @endfor
                            </div>
                        @endif
                    </section>
                @else
                    <p class="team-settings-access-note">Просматривать и отправлять приглашения может только owner команды</p>
                @endif
            </div>
        </section>
    `
)
