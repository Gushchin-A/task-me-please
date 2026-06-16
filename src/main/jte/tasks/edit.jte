@import dev.gushchin.taskmanager.model.TaskCategory
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.User
@import dev.gushchin.taskmanager.view.TaskView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TaskView task
@param Team team
@param List<User> members
@param TaskCategory[] categories
@param CsrfToken _csrf

@template.layout.page(
    title = "Редактировать задачу",
    content = @`
        <section>
            <a href="/tasks/${task.id()}">
                ← Назад к задаче
            </a>

            <h1>Редактировать задачу</h1>

            <form method="post" action="/tasks/${task.id()}/edit">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <div>
                    <label>Название</label>
                    <input type="text" name="title" value="${task.title()}" required>
                </div>

                <div>
                    <label>Описание</label>
                    <textarea name="description">${task.description()}</textarea>
                </div>

                <div>
                    <label>Дедлайн</label>

                    @if(task.deadlineAt() == null)
                        <input type="date" name="deadlineDate">
                    @else
                        <input
                            type="date"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif
                </div>

                <div>
                    <label>Категория</label>

                    <select name="category" required>
                        @for(TaskCategory category : categories)
                            <option
                                value="${category.name()}"
                                selected="${category == task.category()}"
                            >
                                ${category.name()}
                            </option>
                        @endfor
                    </select>
                </div>

                <div>
                    <label>Исполнитель</label>

                    <select name="assigneeId" required>
                        @for(User member : members)
                            <option
                                value="${member.getId().toString()}"
                                selected="${member.getId().equals(task.assigneeId())}"
                            >
                                ${member.getName()}
                            </option>
                        @endfor
                    </select>
                </div>

                <p>Команда: ${team.getName()}</p>

                <button type="submit">
                    Сохранить
                </button>
            </form>
        </section>
    `
)