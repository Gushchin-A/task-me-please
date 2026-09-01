@import dev.gushchin.taskmanager.view.InvitationDecisionView
@import org.springframework.security.web.csrf.CsrfToken

@param InvitationDecisionView invitation
@param CsrfToken _csrf
@param String returnTo = null

<dialog
    class="settings-dialog invitation-decision-dialog"
    aria-labelledby="invitation-decision-title"
    data-invitation-decision-dialog
>
    <div class="settings-dialog-form">
        <header class="settings-dialog-header">
            <h2 id="invitation-decision-title" tabindex="-1" data-invitation-decision-title>
                Приглашение в команду
            </h2>
            <button type="button" aria-label="Закрыть" data-invitation-decision-close>×</button>
        </header>

        <div class="settings-dialog-body">
            <p>
                ${invitation.invitedByEmail()} пригласил вас в команду «${invitation.teamName()}».
            </p>
        </div>

        <footer class="settings-dialog-footer invitation-decision-actions">
            <form method="post" action="/invitations/${invitation.token()}/decline" data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @if(returnTo != null)
                    <input type="hidden" name="returnTo" value="${returnTo}">
                @endif
                <button class="button button-secondary" type="submit" data-loading-text="Обработка...">
                    Отклонить
                </button>
            </form>

            <form method="post" action="/invitations/${invitation.token()}/accept" data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                <button class="button button-primary" type="submit" data-loading-text="Обработка...">
                    Принять
                </button>
            </form>
        </footer>
    </div>
</dialog>
