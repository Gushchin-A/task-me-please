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
    <title>${title}</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
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
                            ${inviteContext.invitedByName()}, ${inviteContext.invitedByEmail()}
                            пригласил вас в команду «${inviteContext.teamName()}».
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
