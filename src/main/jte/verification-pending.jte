@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param boolean limitReached
@param boolean deliveryFailed
@param String redirect
@param String invite

@template.layout.auth(
    title = "Подтверждение почты",
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
                попробуйте запросить подтверждение на следующий день.
            </p>
        @else
            @if(deliveryFailed)
                <p class="auth-description">
                    Мы не смогли отправить письмо по техническим причинам.
                    Можно попробовать снова или вернуться позже.
                </p>
            @else
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
