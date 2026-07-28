@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import dev.gushchin.taskmanager.view.TaskView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TaskView task
@param Team team
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Редактировать задачу",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    content = @`
        <section>
            <a href="/tasks/${task.id()}">
                ← Назад к задаче
            </a>

            <h1>Редактировать задачу</h1>

            <form
                method="post"
                action="/tasks/${task.id()}/edit"
            >
                <input
                    type="hidden"
                    name="${_csrf.getParameterName()}"
                    value="${_csrf.getToken()}"
                >

                <div>
                    <label>Название</label>

                    <input
                        type="text"
                        name="title"
                        value="${task.title()}"
                        required
                    >
                </div>

                <div>
                    <label>Описание</label>

                    <textarea name="description">@if(task.description() != null)${task.description()}@endif</textarea>
                </div>

                <div>
                    <label>Дедлайн</label>

                    @if(task.deadlineAt() == null)
                        <input
                            type="date"
                            name="deadlineDate"
                        >
                    @else
                        <input
                            type="date"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif
                </div>

                <div>
                    <label>Тег</label>

                    <select
                        name="tagId"
                        required
                    >
                        @for(TeamTag tag : tags)
                            <option
                                value="${tag.getId()}"
                                selected="${tag.getId().equals(task.tagId())}"
                            >
                                ${tag.getName()}
                            </option>
                        @endfor
                    </select>
                </div>

                <div>
                    <label>Исполнитель</label>

                    <select
                        name="assigneeId"
                        required
                    >
                        @for(TaskParticipantView member : members)
                            <option
                                value="${member.id().toString()}"
                                selected="${member.id().equals(task.assigneeId())}"
                            >
                                ${member.name()}
                            </option>
                        @endfor
                    </select>
                </div>

                <p>
                    Команда: ${team.getName()}
                </p>

                <button type="submit">
                    Сохранить
                </button>
            </form>
        </section>
    `
)
