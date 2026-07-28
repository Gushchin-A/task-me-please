@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf
@param String loginUrl
@param String registrationUrl
@param String successMessage
@param String errorMessage
@param String email
@param int cooldownSeconds
@param int remainingAttempts
@param boolean requestAvailable

@template.layout.auth(
    title = "Восстановление password",
    loginUrl = loginUrl,
    registrationUrl = registrationUrl,
    activeTab = "",
    invite = null,
    inviteContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <h1 id="auth-heading">Забыли пароль?</h1>

        <p>Введите email, указанный при регистрации.</p>

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

            @if(cooldownSeconds > 0 && remainingAttempts > 0)
                <p data-request-countdown="${cooldownSeconds}">
                    Повторная отправка будет доступна через:
                    <span data-request-seconds>${cooldownSeconds}</span> сек.
                </p>
            @endif

            <p>Повторных попыток осталось: <span data-remaining-attempts>${remainingAttempts}</span></p>

            <button
                class="auth-submit"
                type="submit"
                data-loading-text="Отправляем письмо…"
                disabled="${!requestAvailable}"
            >
                Отправить письмо
            </button>
        </form>

        <details>
            <summary>
                Почему количество попыток в сутки ограничено
            </summary>
            <p>
                Это бета-версия таск-трекера. В сервисе используется ограниченный лимит писем в день.
                Если письмо так и не пришло, попробуйте запросить восстановление на следующий день.
                Или напишите на почту <a href="mailto:support@example.com">support@example.com</a>.
            </p>
        </details>

        <script>
            const countdown = document.querySelector('[data-request-countdown]');

            if (countdown) {
                const secondsElement = countdown.querySelector('[data-request-seconds]');
                const requestButton = document.querySelector('button[type="submit"]');
                let seconds = Number(countdown.dataset.requestCountdown);

                const timer = setInterval(function () {
                    seconds -= 1;
                    secondsElement.textContent = String(Math.max(0, seconds));

                    if (seconds <= 0) {
                        clearInterval(timer);
                        countdown.remove();
                        requestButton.disabled = false;
                    }
                }, 1000);
            }
        </script>
    `
)
