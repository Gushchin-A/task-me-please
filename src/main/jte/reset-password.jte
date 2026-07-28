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
        <h1 id="auth-heading">Установите новый password</h1>

        <form method="post" action="/reset-password/${token}" data-submit-loading>
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

            <div class="auth-field">
                <label for="reset-password">Новый password</label>
                <input id="reset-password" type="password" name="password" autocomplete="new-password" required>
            </div>

            <div class="auth-field">
                <label for="reset-password-confirmation">Повторите новый password</label>
                <input
                    id="reset-password-confirmation"
                    type="password"
                    name="passwordConfirmation"
                    autocomplete="new-password"
                    required
                >
            </div>

            <button class="auth-submit" type="submit" data-loading-text="Изменяем password…">
                Изменить password
            </button>
        </form>
    `
)
