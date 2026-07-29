@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param Long selectedTeamId
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Создать задачу",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    content = @`
        <section class="workspace-page form-page">
            <a class="back-link" href="/tasks">← Вернуться к задачам</a>
            <h1>Создать задачу</h1>

            @if(teams.isEmpty())
                <p>
                    Сначала создайте команду.
                </p>

                <a class="button button-primary" href="/teams/new">
                    Создать команду
                </a>
            @else
                <form class="form-card" method="post" action="/tasks">
                    <input
                        type="hidden"
                        name="${_csrf.getParameterName()}"
                        value="${_csrf.getToken()}"
                    >

                    <div class="form-field">
                        <label for="task-team">Команда</label>

                        <select
                            id="task-team"
                            name="teamId"
                            required
                            onchange="window.location.href = '/tasks/new?teamId=' + this.value"
                        >
                            @for(Team team : teams)
                                <option
                                    value="${team.getId()}"
                                    selected="${selectedTeamId != null
                                        && selectedTeamId.equals(team.getId())}"
                                >
                                    ${team.getName()}
                                </option>
                            @endfor
                        </select>
                    </div>

                    @if(tags.isEmpty())
                        <p>
                            В выбранной команде нет тегов.
                            Создать задачу пока невозможно.
                        </p>
                    @else
                        <div class="form-field">
                            <label for="task-assignee">Исполнитель</label>

                            <select
                                id="task-assignee"
                                name="assigneeId"
                                required
                            >
                                @for(TaskParticipantView member : members)
                                    <option
                                        value="${member.id().toString()}"
                                    >
                                        ${member.name()}
                                    </option>
                                @endfor
                            </select>
                        </div>

                        <div class="form-field">
                            <label for="task-title">Название задачи</label>

                            <input
                                id="task-title"
                                type="text"
                                name="title"
                                required
                            >
                        </div>

                        <div class="form-field">
                            <label for="task-description">Описание</label>

                            <textarea id="task-description" name="description"></textarea>
                        </div>

                        <div class="form-field">
                            <label for="task-deadline">Дедлайн</label>

                            <input
                                id="task-deadline"
                                type="date"
                                name="deadlineDate"
                            >
                        </div>

                        <div class="form-field">
                            <label for="task-tag">Тег</label>

                            <select
                                id="task-tag"
                                name="tagId"
                                required
                            >
                                @for(TeamTag tag : tags)
                                    <option value="${tag.getId()}">
                                        ${tag.getName()}
                                    </option>
                                @endfor
                            </select>
                        </div>

                        <button class="button button-primary" type="submit">
                            Создать задачу
                        </button>
                    @endif
                </form>
            @endif
        </section>
    `
)
