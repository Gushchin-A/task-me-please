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
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <link rel="icon" href="/images/favicon.svg" type="image/svg+xml">
    <link rel="icon" href="/images/favicon-32x32.png" sizes="32x32" type="image/png">
    <link rel="apple-touch-icon" href="/images/favicon-180x180.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        rel="stylesheet"
        href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap"
    >
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
    <main class="auth-page">
        <div class="auth-shell">
            <a class="auth-logo" href="/" aria-label="Task Me Please">
                <img src="/images/logo.png" width="180" height="120" alt="Task Me Please">
            </a>

            <p class="auth-tagline">
                Личные и командные задачи в одном рабочем пространстве
            </p>

            <section class="auth-panel" aria-labelledby="auth-heading">
                @if(!activeTab.isBlank())
                    <nav class="auth-tabs" aria-label="Авторизация">
                        <a
                            href="${loginUrl}"
                            class="auth-tab ${activeTab.equals("login") ? "auth-tab-active" : ""}"
                            aria-current="${activeTab.equals("login") ? "page" : "false"}"
                        >
                            Вход
                        </a>

                        <a
                            href="${registrationUrl}"
                            class="auth-tab ${activeTab.equals("registration") ? "auth-tab-active" : ""}"
                            aria-current="${activeTab.equals("registration") ? "page" : "false"}"
                        >
                            Регистрация
                        </a>
                    </nav>
                @endif

                @if(inviteContext != null)
                    <div class="auth-invite">
                        <strong>Приглашение в команду</strong>
                        <p>
                            Вас пригласил ${inviteContext.invitedByEmail()}
                            в команду «${inviteContext.teamName()}».
                        </p>
                        <p>После входа или регистрации вы вернётесь к приглашению.</p>
                    </div>
                @endif

                ${content}

                @if(activeTab.isBlank())
                    <a class="auth-back-link" href="${loginUrl}">
                        ← Вернуться ко входу
                    </a>
                @endif
            </section>
        </div>
    </main>

    <div class="flash-stack" aria-live="polite" aria-atomic="false">
        @if(successMessage != null)
            @template.components.flash(message = successMessage, type = "success")
        @endif

        @if(errorMessage != null)
            @template.components.flash(message = errorMessage, type = "error")
        @endif
    </div>

    <script src="/js/app.js"></script>
</body>
</html>
