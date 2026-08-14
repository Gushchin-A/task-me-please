@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param List<Team> navigationTeams
@param int step
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — удалить",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = "delete"
        )

        <section class="workspace-page destructive-page">
            <div class="destructive-card">
            <span class="destructive-step">Шаг ${step} из 3</span>
            <h1>Удаление команды «${team.getName()}»</h1>

            @if(step == 1)
                <p>Вы уверены, что хотите удалить команду?</p>
            @elseif(step == 2)
                <p>Все участники потеряют доступ к команде и её задачам. Продолжить?</p>
            @else
                <p>Это последнее подтверждение. Удалить команду?</p>
            @endif

            <form class="destructive-actions" method="post" action="/teams/${team.getId()}/delete">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <button class="button button-danger" type="submit" name="confirmation" value="yes">
                    Да
                </button>

                <button class="button button-secondary" type="submit" name="confirmation" value="no">
                    Нет
                </button>
            </form>
            </div>
        </section>
    `
)
