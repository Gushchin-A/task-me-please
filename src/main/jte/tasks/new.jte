@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.TaskParticipantView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param List<Team> teams
@param List<TaskParticipantView> members
@param List<TeamTag> tags
@param Long selectedTeamId
@param CsrfToken _csrf

@template.layout.page(
    title = "Создать задачу",
    content = @`
        <section>
            <h1>Создать задачу</h1>

            @if(teams.isEmpty())
                <p>
                    Сначала создайте команду.
                </p>

                <a href="/teams/new">
                    Создать команду
                </a>
            @else
                <form method="post" action="/tasks">
                    <input
                        type="hidden"
                        name="${_csrf.getParameterName()}"
                        value="${_csrf.getToken()}"
                    >

                    <div>
                        <label>Команда</label>

                        <select
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
                        <div>
                            <label>Исполнитель</label>

                            <select
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

                        <div>
                            <label>Название задачи</label>

                            <input
                                type="text"
                                name="title"
                                required
                            >
                        </div>

                        <div>
                            <label>Описание</label>

                            <textarea name="description"></textarea>
                        </div>

                        <div>
                            <label>Дедлайн</label>

                            <input
                                type="date"
                                name="deadlineDate"
                            >
                        </div>

                        <div>
                            <label>Тег</label>

                            <select
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

                        <button type="submit">
                            Создать задачу
                        </button>
                    @endif
                </form>
            @endif
        </section>
    `
)
