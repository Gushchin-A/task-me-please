@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String email
@param String redirect
@param String invite
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param int cooldownSeconds
@param int remainingAttempts
@param boolean resendAvailable

@template.layout.auth(
    title = "Подтверждение email",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "",
    invite = invite,
    inviteContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <h1>Подтвердите email</h1>

        <p>Мы отправили письмо на ${email}.</p>
        <p>Перейдите по ссылке в письме, чтобы подтвердить email и завершить регистрацию.</p>
        <p>Если письма нет, проверьте папку «Спам».</p>

        @if(cooldownSeconds > 0 && remainingAttempts > 0)
            <p data-resend-countdown="${cooldownSeconds}">
                Повторная отправка будет доступна через:
                <span data-resend-seconds>${cooldownSeconds}</span> сек.
            </p>
        @endif

        <p>Повторных попыток осталось: <span data-remaining-attempts>${remainingAttempts}</span></p>

        <form method="post" action="/resend-verification">
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            <input type="hidden" name="email" value="${email}">

            @if(redirect != null)
                <input type="hidden" name="redirect" value="${redirect}">
            @endif

            @if(invite != null)
                <input type="hidden" name="invite" value="${invite}">
            @endif

            <button class="auth-submit" type="submit" disabled="${!resendAvailable}">
                Отправить письмо повторно
            </button>
        </form>

        <details>
            <summary style="color: #57606a; cursor: pointer;">
                Почему количество попыток в сутки ограничено
            </summary>
            <p>
                Это бета-версия таск-трекера. В сервисе используется ограниченный лимит писем в день.
                Если письмо так и не пришло, попробуйте зарегистрироваться на следующий день.
                Или напишите на почту <a href="mailto:support@example.com">support@example.com</a>.
            </p>
        </details>

        <script>
            const countdown = document.querySelector('[data-resend-countdown]');

            if (countdown) {
                const secondsElement = countdown.querySelector('[data-resend-seconds]');
                const resendButton = document.querySelector('button[type="submit"]');
                let seconds = Number(countdown.dataset.resendCountdown);

                const timer = setInterval(function () {
                    seconds -= 1;
                    secondsElement.textContent = String(Math.max(0, seconds));

                    if (seconds <= 0) {
                        clearInterval(timer);
                        countdown.remove();
                        resendButton.disabled = false;
                    }
                }, 1000);
            }
        </script>
    `
)
