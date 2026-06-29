@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTaskVisibility
@import dev.gushchin.taskmanager.view.TeamMemberView
@import java.util.List
@import java.util.UUID
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param int totalTasksCount
@param int membersCount
@param List<TeamMemberView> members
@param boolean onlyCurrentOwnerInTeam
@param boolean canInvite
@param boolean canManageVisibility
@param boolean canSeeMemberDetails
@param boolean canSeeMemberPrivateData
@param UUID currentUserId
@param TeamTaskVisibility[] taskVisibilities
@param CsrfToken _csrf

@template.layout.page(
    title = team.getName() + " — участники",
    content = @`
        <section>
            <h1>${team.getName()}</h1>

            <div>
                <a href="/teams/${team.getId()}">
                    Задачи (${totalTasksCount})
                </a>

                <a href="/teams/${team.getId()}/members">
                    <strong>Участники (${membersCount})</strong>
                </a>

                @if(canInvite)
                    <a href="/teams/${team.getId()}/invite">
                        Пригласить в команду
                    </a>
                @else
                    <a
                        href="#"
                        onclick="showFlashMessage('Только owner команды может приглашать новых участников. Вы можете пока только просматривать команду.'); return false;"
                    >
                        Пригласить в команду
                    </a>
                @endif
            </div>

            @if(onlyCurrentOwnerInTeam)
                <div>
                    <p>Кроме вас в команде пока никого нет.</p>

                    @if(canInvite)
                        <a href="/teams/${team.getId()}/invite">
                            Пригласить в команду
                        </a>
                    @else
                        <a
                            href="#"
                            onclick="showFlashMessage('Только owner команды может приглашать новых участников. Вы можете пока только просматривать команду.'); return false;"
                        >
                            Пригласить в команду
                        </a>
                    @endif
                </div>
            @else
                @if(members.isEmpty())
                    <div>
                        <p>В команде нет участников.</p>

                        @if(canInvite)
                            <a href="/teams/${team.getId()}/invite">
                                Пригласить в команду
                            </a>
                        @else
                            <a
                                href="#"
                                onclick="showFlashMessage('Только owner команды может приглашать новых участников. Вы можете пока только просматривать команду.'); return false;"
                            >
                                Пригласить в команду
                            </a>
                        @endif
                    </div>
                @else
                    <table>
                        <thead>
                            <tr>
                                <th>Имя</th>

                                @if(canSeeMemberPrivateData)
                                    <th>Почта</th>
                                    <th>Роль в команде</th>
                                @endif

                                @if(canSeeMemberDetails)
                                    <th>Кол-во задач, где автор</th>
                                    <th>Кол-во задач, где исполнитель</th>
                                @endif

                                @if(canManageVisibility)
                                    <th>Видимость задач</th>
                                @endif
                            </tr>
                        </thead>

                        <tbody>
                            @for(TeamMemberView member : members)
                                <tr>
                                    <td>${member.name()}</td>

                                    @if(canSeeMemberPrivateData)
                                        <td>${member.email()}</td>
                                        <td>${member.role().name()}</td>
                                    @endif

                                    @if(canSeeMemberDetails)
                                        <td>${member.authorTasksCount()}</td>
                                        <td>${member.assigneeTasksCount()}</td>
                                    @endif

                                    @if(canManageVisibility)
                                        <td>
                                            @if(member.userId().equals(currentUserId))
                                                <span>Видит все задачи</span>
                                            @else
                                                <form
                                                    method="post"
                                                    action="/teams/${team.getId()}/members/${member.userId().toString()}/visibility"
                                                    style="display: inline;"
                                                    data-inline-update-form="true"
                                                >
                                                    <input
                                                        type="hidden"
                                                        name="${_csrf.getParameterName()}"
                                                        value="${_csrf.getToken()}"
                                                    >

                                                    <select
                                                        name="taskVisibility"
                                                        data-inline-control
                                                        data-original-value="${member.taskVisibility().name()}"
                                                        onchange="showInlineSavePanel(this)"
                                                    >
                                                        @for(TeamTaskVisibility taskVisibility : taskVisibilities)
                                                            @if(taskVisibility == member.taskVisibility())
                                                                <option value="${taskVisibility.name()}" selected>
                                                                    @if(taskVisibility == TeamTaskVisibility.ALL_TASKS)
                                                                        Видит все задачи
                                                                    @else
                                                                        Видит только свои задачи
                                                                    @endif
                                                                </option>
                                                            @else
                                                                <option value="${taskVisibility.name()}">
                                                                    @if(taskVisibility == TeamTaskVisibility.ALL_TASKS)
                                                                        Видит все задачи
                                                                    @else
                                                                        Видит только свои задачи
                                                                    @endif
                                                                </option>
                                                            @endif
                                                        @endfor
                                                    </select>

                                                    <span data-inline-actions style="display: none;">
                                                        <button type="submit">
                                                            Сохранить
                                                        </button>

                                                        <button type="button" onclick="cancelInlineUpdate(this)">
                                                            Отмена
                                                        </button>
                                                    </span>
                                                </form>
                                            @endif
                                        </td>
                                    @endif
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                @endif
            @endif
        </section>
    `
)