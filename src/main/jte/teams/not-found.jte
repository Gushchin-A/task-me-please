@import dev.gushchin.taskmanager.view.UserAvatarView

@param UserAvatarView currentUser

@template.layout.page(
    title = "Страница не найдена",
    _csrf = null,
    currentUser = currentUser,
    activeSection = "teams",
    content = @`
        <section class="workspace-page form-page empty-state">
            <h1>Такая страница не найдена</h1>

            <p>Возможно, ссылка устарела или такой страницы не существует.</p>

            <a class="button button-secondary" href="/teams">
                Вернуться к командам
            </a>
        </section>
    `
)
