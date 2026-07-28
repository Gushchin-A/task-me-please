@import gg.jte.Content
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param String title
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String activeSection
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
    <div class="app-shell">
        <aside class="app-sidebar">
            <a class="app-logo" href="/tasks" aria-label="Task Me Please — мои задачи">
                <img src="/images/logo.png" width="142" height="72" alt="Task Me Please">
            </a>

            <nav class="app-nav" aria-label="Основная навигация">
                <a
                    class="app-nav-link ${"tasks".equals(activeSection) ? "app-nav-link-active" : ""}"
                    href="/tasks"
                    aria-current="${"tasks".equals(activeSection) ? "page" : "false"}"
                >
                    Мои задачи
                </a>

                <a
                    class="app-nav-link ${"teams".equals(activeSection) ? "app-nav-link-active" : ""}"
                    href="/teams"
                    aria-current="${"teams".equals(activeSection) ? "page" : "false"}"
                >
                    Мои команды
                </a>
            </nav>
        </aside>

        <div class="app-main">
            <header class="app-topbar">
                <div class="app-topbar-actions">
                    <a class="button button-primary" href="/tasks/new">Создать задачу</a>
                    <a class="button button-secondary" href="/teams/new">Создать команду</a>
                </div>

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
                                <button
                                    class="profile-action"
                                    type="button"
                                    title="Настройки пока не реализованы"
                                    disabled
                                >
                                    Настройки
                                    <span class="profile-action-note">Настройки пока не реализованы</span>
                                </button>

                                @if(_csrf != null)
                                    <form class="profile-logout-form" method="post" action="/logout">
                                        <input
                                            type="hidden"
                                            name="${_csrf.getParameterName()}"
                                            value="${_csrf.getToken()}"
                                        >

                                        <button class="profile-action" type="submit">
                                            Выйти из профиля
                                        </button>
                                    </form>
                                @endif
                            </div>
                        </div>
                    </div>
                @endif
            </header>

            <main class="app-content">
                ${content}
            </main>
        </div>
    </div>

    <div class="flash-stack" aria-live="polite" aria-atomic="false"></div>

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
