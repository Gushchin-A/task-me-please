@import gg.jte.Content
@import dev.gushchin.taskmanager.view.AuthenticationInviteView

@param String title
@param String loginUrl
@param String registrationUrl
@param String activeTab
@param String invite
@param AuthenticationInviteView inviteContext
@param String successMessage
@param String errorMessage
@param Content content

<!doctype html>
<html lang="ru" data-color-mode="auto" data-light-theme="light" data-dark-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <link rel="icon" href="/images/favicon.svg" type="image/svg+xml">
    <link rel="icon" href="/images/favicon-32x32.png" sizes="32x32" type="image/png">
    <link rel="apple-touch-icon" href="/images/favicon-180x180.png">
    <link rel="stylesheet" href="/css/primer/primitives.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/light.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/dark.css">
    <link rel="stylesheet" href="/css/app.css">
</head>
<body class="auth-document">
    <div class="flash-stack app-flash-stack" aria-live="polite" aria-atomic="false">
        @if(successMessage != null)
            @template.components.flash(message = successMessage, type = "success")
        @endif

        @if(errorMessage != null)
            @template.components.flash(message = errorMessage, type = "error")
        @endif
    </div>

    <main class="auth-page">
        <div class="auth-shell">
            <a class="auth-logo app-logo" href="/" aria-label="Task Me Please">TASKMEPLEASE</a>

            @if(!activeTab.isBlank()
                    && !activeTab.equals("forgot-password-limited")
                    && !activeTab.equals("verification-limited"))
                <h1 class="auth-title">${title}</h1>
            @endif

            <section class="auth-panel" aria-labelledby="${activeTab.isBlank() ? "auth-heading" : null}">
                @if(inviteContext != null)
                    <div class="auth-invite">
                        <p>Вас пригласили в команду «${inviteContext.teamName()}».</p>
                        <p>После входа в аккаунт или регистрации вы вернетесь к приглашению.</p>
                    </div>
                @endif

                ${content}
            </section>

            @if(activeTab.equals("login"))
                <p class="auth-switch">
                    Нет аккаунта?
                    <a href="${registrationUrl}">Создать аккаунт</a>
                </p>
            @elseif(activeTab.equals("registration"))
                <p class="auth-switch">
                    Уже есть аккаунт?
                    <a href="${loginUrl}">Войти</a>
                </p>
            @elseif(activeTab.equals("forgot-password"))
                <p class="auth-switch">
                    Есть аккаунт?
                    <a href="${loginUrl}">Войти</a>
                </p>
            @elseif(activeTab.equals("forgot-password-sent")
                    || activeTab.equals("forgot-password-limited"))
                <p class="auth-switch">
                    <a href="${loginUrl}">Назад на страницу входа</a>
                </p>
            @elseif(!activeTab.equals("verification-pending")
                    && !activeTab.equals("verification-limited")
                    && !activeTab.equals("reset-password-invalid"))
                <a class="auth-back-link" href="${loginUrl}">
                    ← Вернуться назад
                </a>
            @endif
        </div>
    </main>

    @template.components.siteFooter()

    <script src="/js/app.js"></script>
</body>
</html>
