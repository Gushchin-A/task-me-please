@param boolean authenticated

<!doctype html>
<html lang="ru" data-color-mode="auto" data-light-theme="light" data-dark-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Приглашение недействительно</title>
    <link rel="icon" href="/images/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="/css/primer/primitives.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/light.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/dark.css">
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
    <main class="invitation-page">
        <section class="invitation-panel">
            <h1>Ссылка приглашения недействительна.</h1>

            <div class="invitation-actions">
                @if(authenticated)
                    <a class="button button-primary" href="/tasks">
                        К моим задачам
                    </a>
                @else
                    <a class="button button-primary" href="/login">
                        Войти
                    </a>

                    <a class="button button-secondary" href="/registration">
                        Зарегистрироваться
                    </a>
                @endif
            </div>
        </section>
    </main>
</body>
</html>
