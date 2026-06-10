@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf

@template.layout.page(
    title = "Создать команду",
    content = @`
        <section>
            <h1>Создать команду</h1>

            <form method="post" action="/teams">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <div>
                    <label>Название команды</label>
                    <input type="text" name="name" required>
                </div>

                <button type="submit">Создать</button>
            </form>
        </section>
    `
)