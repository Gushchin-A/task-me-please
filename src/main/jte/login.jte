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
    title = "Войдите, чтобы продолжить",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "login",
    invite = invite,
    inviteContext = inviteContext,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <form method="post" action="/login" data-submit-loading>
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            @if(redirect != null)
                <input type="hidden" name="redirect" value="${redirect}">
            @endif

            @if(invite != null)
                <input type="hidden" name="invite" value="${invite}">
            @endif

            <div class="auth-field">
                <label for="login-email">Email</label>
                <input id="login-email" type="email" name="username" autocomplete="email" required>
            </div>

            <div class="auth-field">
                <div class="auth-label-row">
                    <label for="login-password">Пароль</label>
                    <a class="auth-label-link" href="/forgot-password">Забыли пароль?</a>
                </div>
                <input id="login-password" type="password" name="password" autocomplete="current-password" required>
            </div>

            <label class="auth-checkbox">
                <input type="checkbox" name="remember-me">
                Запомнить меня
            </label>

            <button class="auth-submit" type="submit" data-loading-text="Выполняется вход…">
                Войти
            </button>
        </form>

        @if(unverifiedEmail != null)
            <a class="auth-submit" href="/verification-pending">
                Перейти к повторной отправке
            </a>
        @endif
    `
)
