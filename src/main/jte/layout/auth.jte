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

    <style>
        body {
            margin: 0;
            font-family: Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            background: #f6f8fa;
        }

        .auth-page {
            min-height: 100vh;
            display: grid;
            grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
        }

        .auth-brand {
            border-right: 1px solid #d0d7de;
            padding: 40px;
            background: white;
        }

        .auth-brand-title {
            font-size: 28px;
            font-weight: 700;
            margin-bottom: 16px;
        }

        .auth-content {
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px;
        }

        .auth-panel {
            width: 100%;
            max-width: 420px;
            border: 1px solid #d0d7de;
            border-radius: 8px;
            background: white;
            padding: 24px;
        }

        .auth-tabs {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 24px;
        }

        .auth-tab {
            border: 1px solid #d0d7de;
            border-radius: 6px;
            color: #24292f;
            padding: 10px;
            text-align: center;
            text-decoration: none;
        }

        .auth-tab-active {
            border-color: #24292f;
            font-weight: 700;
        }

        .auth-field {
            display: grid;
            gap: 6px;
            margin-bottom: 16px;
        }

        .auth-field input {
            border: 1px solid #d0d7de;
            border-radius: 6px;
            font-size: 16px;
            padding: 10px;
        }

        .auth-submit {
            border: 1px solid #24292f;
            border-radius: 6px;
            background: #24292f;
            color: white;
            cursor: pointer;
            font-size: 16px;
            padding: 10px 14px;
            width: 100%;
        }

        .auth-invite {
            border: 1px solid #d0d7de;
            border-radius: 6px;
            margin-bottom: 20px;
            padding: 12px;
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

        @media (max-width: 760px) {
            .auth-page {
                grid-template-columns: 1fr;
            }

            .auth-brand {
                border-right: 0;
                border-bottom: 1px solid #d0d7de;
                padding: 24px;
            }
        }
    </style>
</head>
<body>
    <main class="auth-page">
        <section class="auth-brand">
            <div class="auth-brand-title">Task Me Please</div>
            <p>Личные и командные задачи в одном рабочем пространстве.</p>
        </section>

        <section class="auth-content">
            <div class="auth-panel">
                <div class="auth-tabs">
                    <a
                        href="${loginUrl}"
                        class="auth-tab ${activeTab.equals("login") ? "auth-tab-active" : ""}"
                    >
                        Вход
                    </a>

                    <a
                        href="${registrationUrl}"
                        class="auth-tab ${activeTab.equals("registration") ? "auth-tab-active" : ""}"
                    >
                        Регистрация
                    </a>
                </div>

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

                @if(successMessage != null)
                    <div class="flash-message">
                        ${successMessage}
                    </div>
                @endif

                @if(errorMessage != null)
                    <div class="flash-message">
                        ${errorMessage}
                    </div>
                @endif

                ${content}
            </div>
        </section>
    </main>

    <script>
        setTimeout(function () {
            const messages = document.querySelectorAll('.flash-message');

            messages.forEach(function (message) {
                message.remove();
            });
        }, 2000);
    </script>
</body>
</html>
