@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param boolean limitReached

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
            <p class="auth-description">
                Отправили вам ссылку для подтверждения. Если письмо не появится в течение нескольких минут,
                проверьте папку «Спам».
            </p>
        @endif
    `
)
