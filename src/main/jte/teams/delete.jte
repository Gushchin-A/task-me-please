@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param int step
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — удалить",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    content = @`
        <section>
            <h1>Удаление команды «${team.getName()}»</h1>

            @if(step == 1)
                <p>Вы уверены, что хотите удалить команду?</p>
            @elseif(step == 2)
                <p>Все участники потеряют доступ к команде и её задачам. Продолжить?</p>
            @else
                <p>Это последнее подтверждение. Удалить команду?</p>
            @endif

            <form method="post" action="/teams/${team.getId()}/delete">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <button type="submit" name="confirmation" value="yes">
                    Да
                </button>

                <button type="submit" name="confirmation" value="no">
                    Нет
                </button>
            </form>
        </section>
    `
)
