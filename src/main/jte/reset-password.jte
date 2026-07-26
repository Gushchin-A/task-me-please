@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String token
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage

@template.layout.auth(
    title = "Новый password",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "",
    invite = null,
    inviteContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <h1>Установите новый password</h1>

        <form method="post" action="/reset-password/${token}">
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            <div class="auth-field">
                <label>Новый password</label>
                <input type="password" name="password" required>
            </div>

            <div class="auth-field">
                <label>Повторите новый password</label>
                <input type="password" name="passwordConfirmation" required>
            </div>

            <button class="auth-submit" type="submit">
                Изменить password
            </button>
        </form>
    `
)
