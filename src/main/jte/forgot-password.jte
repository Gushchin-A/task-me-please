@import dev.gushchin.taskmanager.model.PasswordResetRequestResult
@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param String email
@param PasswordResetRequestResult requestResult

!{String activeTab = requestResult == PasswordResetRequestResult.LIMIT_REACHED
        ? "forgot-password-limited"
        : requestResult == PasswordResetRequestResult.SENT ? "forgot-password-sent" : "forgot-password";}

@template.layout.auth(
    title = "Сбросить пароль",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = activeTab,
    invite = null,
    inviteContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        @if(requestResult == PasswordResetRequestResult.LIMIT_REACHED)
            <p class="auth-description auth-description--standalone">
                В сервисе используется ограниченный лимит писем в день. Если письмо так и не пришло,
                попробуйте запросить восстановление на следующий день.
            </p>
        @elseif(requestResult == PasswordResetRequestResult.SENT)
            <p class="auth-description">
                Отправили вам ссылку для сброса пароля. Если письмо не появится в течение нескольких минут,
                проверьте папку «Спам».
            </p>
        @else
            <p class="auth-description">
                Введите адрес электронной почты вашей учетной записи, и мы вышлем вам ссылку для сброса пароля.
            </p>

            <form method="post" action="/forgot-password" data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <div class="auth-field">
                    <label for="forgot-password-email">Email</label>
                    <input
                        id="forgot-password-email"
                        type="email"
                        name="email"
                        value="${email}"
                        autocomplete="email"
                        required
                    >
                </div>

                <button class="auth-submit" type="submit" data-loading-text="Отправляем письмо…">
                    Отправить письмо
                </button>
            </form>
        @endif
    `
)
