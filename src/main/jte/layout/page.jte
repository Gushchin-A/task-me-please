@import gg.jte.Content

@param String title
@param Content content

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>${title}</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
        }

        .page {
            display: grid;
            grid-template-columns: 220px 1fr;
            grid-template-rows: 88px 1fr;
            min-height: 100vh;
        }

        .topbar {
            grid-column: 2;
            grid-row: 1;
            border-bottom: 1px solid #d0d7de;
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 0 24px;
        }

        .sidebar {
            grid-column: 1;
            grid-row: 1 / 3;
            border-right: 1px solid #d0d7de;
            padding: 24px;
        }

        .content {
            grid-column: 2;
            grid-row: 2;
            padding: 24px;
        }

        .flash-message {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            padding: 12px 16px;
            border: 1px solid #d0d7de;
            background: white;
            box-shadow: 0 8px 24px rgba(140, 149, 159, 0.2);
            z-index: 1000;
        }
    </style>
</head>
<body>
    <div class="page">
        <aside class="sidebar">
            <div>ЛОГО</div>

            <nav>
                <div>
                    <a href="/tasks">Мои задачи</a>
                </div>

                <div>
                    <a href="/teams">Мои команды</a>
                </div>
            </nav>
        </aside>

        <header class="topbar">
            <a href="/tasks/new">Создать задачу</a>
            <a href="/teams/new">Создать команду</a>
        </header>

        <main class="content">
            ${content}
        </main>
    </div>

    <script>
        function showFlashMessage(message) {
            const existingMessages = document.querySelectorAll('.flash-message');

            existingMessages.forEach(function (existingMessage) {
                existingMessage.remove();
            });

            const flashMessage = document.createElement('div');
            flashMessage.className = 'flash-message';
            flashMessage.textContent = message;

            document.body.appendChild(flashMessage);

            setTimeout(function () {
                flashMessage.remove();
            }, 2000);
        }

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

        setTimeout(function () {
            const messages = document.querySelectorAll('.flash-message');

            messages.forEach(function (message) {
                message.remove();
            });
        }, 2000);
    </script>
</body>
</html>