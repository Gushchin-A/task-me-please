@param String title
@param String message

<!doctype html>
<html lang="ru" data-color-mode="auto" data-light-theme="light" data-dark-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} · Task Me Please</title>
    <link rel="icon" href="/images/favicon.svg" type="image/svg+xml">
    <link rel="icon" href="/images/favicon-32x32.png" sizes="32x32" type="image/png">
    <link rel="apple-touch-icon" href="/images/favicon-180x180.png">
    <link rel="stylesheet" href="/css/primer/primitives.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/light.css">
    <link rel="stylesheet" href="/css/primer/functional/themes/dark.css">
    <link rel="stylesheet" href="/css/app.css">
</head>
<body>
    <main class="error-page">
        <a class="error-page-logo" href="/" aria-label="Task Me Please — главная">
            TASKMEPLEASE
        </a>

        <section class="error-page-content" aria-labelledby="error-page-title">
            <h1 id="error-page-title">${title}</h1>

            @if(!message.isBlank())
                <p>${message}</p>
            @endif

            <a class="button button-primary error-page-action" href="/">Вернуться на главную</a>
        </section>
    </main>
</body>
</html>
