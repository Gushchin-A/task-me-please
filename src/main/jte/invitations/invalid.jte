@param boolean authenticated

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Приглашение недействительно</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f6f8fa;
        }

        .invitation-page {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px;
        }

        .invitation-panel {
            width: 100%;
            max-width: 420px;
            border: 1px solid #d0d7de;
            border-radius: 8px;
            background: white;
            padding: 24px;
        }

        .invitation-actions {
            display: flex;
            gap: 12px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <main class="invitation-page">
        <section class="invitation-panel">
            <h1>Ссылка приглашения недействительна.</h1>

            <div class="invitation-actions">
                @if(authenticated)
                    <a href="/tasks">
                        К моим задачам
                    </a>
                @else
                    <a href="/login">
                        Войти
                    </a>

                    <a href="/registration">
                        Зарегистрироваться
                    </a>
                @endif
            </div>
        </section>
    </main>
</body>
</html>
