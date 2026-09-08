@import gg.jte.Content
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param String title
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String activeSection
@param Content headerContext
@param Content content
@param String successMessage = null
@param String errorMessage = null

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
<body>
    <a class="skip-link" href="#main-content">Перейти к содержимому</a>

    <div class="app-shell">
        <header class="app-header">
            <div class="app-header-inner">
                <a class="app-logo" href="/tasks" aria-label="Task Me Please — мои задачи">
                    TASKMEPLEASE
                </a>

                @if(headerContext != null)
                    ${headerContext}
                @else
                    <nav class="app-nav" aria-label="Основная навигация">
                    <a
                        class="app-nav-link ${"tasks".equals(activeSection) ? "app-nav-link-active" : ""}"
                        href="/tasks"
                        aria-current="${"tasks".equals(activeSection) ? "page" : null}"
                    >
                        Мои задачи
                    </a>

                    <a
                        class="app-nav-link ${"teams".equals(activeSection) ? "app-nav-link-active" : ""}"
                        href="/teams"
                        aria-current="${"teams".equals(activeSection) ? "page" : null}"
                    >
                        Мои команды
                    </a>
                    </nav>
                @endif

                @if(currentUser != null)
                    <div class="app-profile" data-profile>
                        <button
                            class="profile-trigger"
                            type="button"
                            data-tooltip="Ваш профиль"
                            aria-label="Ваш профиль"
                            aria-haspopup="true"
                            aria-expanded="false"
                            aria-controls="profile-menu"
                            data-profile-trigger
                        >
                            <span data-profile-initial>${currentUser.initial()}</span>
                            @if(currentUser.hasUnreadNotifications())
                                <span class="profile-unread-indicator" aria-hidden="true"></span>
                            @endif
                        </button>

                        <div class="profile-menu" id="profile-menu" data-profile-menu hidden>
                            <div class="profile-summary">
                                <span class="profile-avatar" aria-hidden="true">
                                    ${currentUser.initial()}
                                </span>

                                <div class="profile-identity">
                                    <span class="profile-name" data-profile-display-name>
                                        ${currentUser.displayName()}
                                    </span>

                                    @if(!currentUser.emailAsDisplayName())
                                        <span class="profile-email" data-profile-email>
                                            ${currentUser.email()}
                                        </span>
                                    @endif
                                </div>
                            </div>

                            <div class="profile-actions">
                                <a class="profile-action" href="/tasks" data-profile-focus>
                                    Мои задачи
                                </a>

                                <a class="profile-action" href="/teams" data-profile-focus>
                                    Мои команды
                                </a>

                                <a class="profile-action" href="/notifications" data-profile-focus>
                                    Уведомления
                                    @if(currentUser.hasUnreadNotifications())
                                        <span class="profile-menu-unread-indicator" aria-hidden="true"></span>
                                        <span class="visually-hidden">Есть непрочитанные уведомления</span>
                                    @endif
                                </a>

                                <a class="profile-action" href="/settings" data-profile-focus>
                                    Настройки
                                </a>

                                @if(_csrf != null)
                                    <form class="profile-logout-form" method="post" action="/logout">
                                        <input
                                            type="hidden"
                                            name="${_csrf.getParameterName()}"
                                            value="${_csrf.getToken()}"
                                        >

                                        <button class="profile-action" type="submit" data-profile-focus>
                                            Выйти из профиля
                                        </button>
                                    </form>
                                @endif
                            </div>
                        </div>
                    </div>
                @endif
            </div>
        </header>

        <div class="flash-stack app-flash-stack" aria-live="polite" aria-atomic="false">
            @if(successMessage != null)
                @template.components.flash(message = successMessage, type = "success")
            @endif

            @if(errorMessage != null)
                @template.components.flash(message = errorMessage, type = "error")
            @endif
        </div>

        <main class="app-content app-content-wide" id="main-content">
            ${content}
        </main>

        @template.components.siteFooter()
    </div>

    <script>
        function resetPendingInlineForms(exceptForm) {
            const forms = document.querySelectorAll('[data-inline-update-form]');

            forms.forEach(function (form) {
                if (form === exceptForm) {
                    return;
                }

                if (form.dataset.dirty !== 'true') {
                    return;
                }

                const control = form.querySelector('[data-inline-control]');
                const actions = form.querySelector('[data-inline-actions]');

                if (control !== null) {
                    control.value = control.dataset.originalValue;
                }

                if (actions !== null) {
                    actions.style.display = 'none';
                }

                form.dataset.dirty = 'false';
            });
        }

        function showInlineSavePanel(control) {
            const form = control.closest('form');

            resetPendingInlineForms(form);

            const actions = form.querySelector('[data-inline-actions]');

            if (actions !== null) {
                actions.style.display = 'inline';
            }

            form.dataset.dirty = 'true';
        }

        function cancelInlineUpdate(button) {
            const form = button.closest('form');
            const control = form.querySelector('[data-inline-control]');
            const actions = form.querySelector('[data-inline-actions]');

            if (control !== null) {
                control.value = control.dataset.originalValue;
            }

            if (actions !== null) {
                actions.style.display = 'none';
            }

            form.dataset.dirty = 'false';
        }

    </script>
    <script src="/js/app.js"></script>
</body>
</html>
