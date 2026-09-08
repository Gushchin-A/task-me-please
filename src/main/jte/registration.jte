@import org.springframework.security.web.csrf.CsrfToken
@import dev.gushchin.taskmanager.view.AuthenticationInviteView

@param CsrfToken _csrf
@param String redirect
@param String invite
@param AuthenticationInviteView inviteContext
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage

@template.layout.auth(
    title = "Создайте учетную запись",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "registration",
    invite = invite,
    inviteContext = inviteContext,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <form method="post" action="/registration" data-submit-loading>
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            @if(redirect != null)
                <input type="hidden" name="redirect" value="${redirect}">
            @endif

            @if(invite != null)
                <input type="hidden" name="invite" value="${invite}">
            @endif

            <div class="auth-field">
                <label for="registration-name">Имя</label>
                <input id="registration-name" type="text" name="name" autocomplete="name">
            </div>

            <div class="auth-field">
                <label for="registration-email">Email</label>
                <input id="registration-email" type="email" name="email" autocomplete="email" required>
            </div>

            <div class="auth-field">
                <label for="registration-password">Пароль</label>
                <input
                    id="registration-password"
                    type="password"
                    name="password"
                    autocomplete="new-password"
                    required
                >
            </div>

            <button class="auth-submit" type="submit" data-loading-text="Создаем аккаунт…">
                Создать аккаунт
            </button>
        </form>
    `
)
