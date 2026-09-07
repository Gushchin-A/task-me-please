@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param boolean limitReached
@param String redirect
@param String invite

@template.layout.auth(
    title = "Подтвердите email",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = limitReached ? "verification-limited" : "verification-pending",
    invite = null,
    inviteContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        @if(limitReached)
            <p class="auth-description auth-description--standalone">
                В сервисе используется ограниченный лимит писем в день. Если письмо так и не пришло,
                попробуйте запросить восстановление на следующий день.
            </p>
        @else
            @if(errorMessage == null)
                <p class="auth-description">
                    Отправили вам ссылку для подтверждения. Если письмо не появится в течение нескольких минут,
                    проверьте папку «Спам».
                </p>
            @endif

            <form method="post" action="/resend-verification" data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                @if(redirect != null)
                    <input type="hidden" name="redirect" value="${redirect}">
                @endif

                @if(invite != null)
                    <input type="hidden" name="invite" value="${invite}">
                @endif

                <button class="auth-submit" type="submit" data-loading-text="Отправляем письмо…">
                    Отправить письмо повторно
                </button>
            </form>
        @endif
    `
)
