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
    </style>
</head>
<body>
    <div class="page">
        <aside class="sidebar">
            <div>ЛОГО</div>

            <nav>
                <a href="/tasks">Задачи</a>
                <a href="/teams">Команды</a>
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
</body>
</html>