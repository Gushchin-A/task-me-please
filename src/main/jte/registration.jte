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
    title = "Регистрация",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "registration",
    invite = invite,
    inviteContext = inviteContext,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <h1>Регистрация</h1>

        <form method="post" action="/registration">
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            @if(redirect != null)
                <input type="hidden" name="redirect" value="${redirect}">
            @endif

            @if(invite != null)
                <input type="hidden" name="invite" value="${invite}">
            @endif

            <div class="auth-field">
                <label>Имя</label>
                <input type="text" name="name">
            </div>

            <div class="auth-field">
                <label>Email</label>
                <input type="email" name="email" required>
            </div>

            <div class="auth-field">
                <label>Пароль</label>
                <input type="password" name="password" required>
            </div>

            <button class="auth-submit" type="submit">
                Создать аккаунт
            </button>
        </form>
    `
)
