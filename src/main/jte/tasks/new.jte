@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TaskCategory
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param Long selectedTeamId
@param TaskCategory[] categories
@param CsrfToken _csrf

@template.layout.page(
    title = "Создать задачу",
    content = @`
        <section>
            <h1>Создать задачу</h1>

            <form method="post" action="/tasks">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <div>
                    <label>Команда</label>

                    <select name="teamId" required>
                        @for(var team : teams)
                            <option
                                value="${team.getId()}"
                                selected="${selectedTeamId != null && selectedTeamId.equals(team.getId())}"
                            >
                                ${team.getName()}
                            </option>
                        @endfor
                    </select>
                </div>

                <div>
                    <label>Название задачи</label>
                    <input type="text" name="title" required>
                </div>

                <div>
                    <label>Описание</label>
                    <textarea name="description"></textarea>
                </div>

                <div>
                    <label>Дедлайн</label>
                    <input type="date" name="deadlineDate">
                </div>

                <div>
                    <label>Категория</label>

                    <select name="category" required>
                        @for(var category : categories)
                            <option value="${category.name()}">${category.name()}</option>
                        @endfor
                    </select>
                </div>

                <button type="submit">Создать задачу</button>
            </form>
        </section>
    `
)