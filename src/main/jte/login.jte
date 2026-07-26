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
@param String unverifiedEmail

@template.layout.auth(
    title = "Вход",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "login",
    invite = invite,
    inviteContext = inviteContext,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <h1>Вход</h1>

        <form method="post" action="/login">
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            @if(redirect != null)
                <input type="hidden" name="redirect" value="${redirect}">
            @endif

            @if(invite != null)
                <input type="hidden" name="invite" value="${invite}">
            @endif

            <div class="auth-field">
                <label>Email</label>
                <input type="email" name="username" required>
            </div>

            <div class="auth-field">
                <label>Пароль</label>
                <input type="password" name="password" required>
            </div>

            <button class="auth-submit" type="submit">
                Войти
            </button>
        </form>

        <p>
            <a href="/forgot-password">Забыли пароль?</a>
        </p>

        @if(unverifiedEmail != null)
            <a class="auth-submit" href="/verification-pending">
                Перейти к повторной отправке
            </a>
        @endif
    `
)
