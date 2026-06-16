@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.TeamMemberView
@import java.util.List

@param Team team
@param int totalTasksCount
@param int membersCount
@param List<TeamMemberView> members
@param boolean onlyCurrentOwnerInTeam
@param boolean canInvite

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
                                <th>Почта</th>
                                <th>Роль в команде</th>
                                <th>Кол-во задач, где автор</th>
                                <th>Кол-во задач, где исполнитель</th>
                            </tr>
                        </thead>

                        <tbody>
                            @for(TeamMemberView member : members)
                                <tr>
                                    <td>${member.name()}</td>
                                    <td>${member.email()}</td>
                                    <td>${member.role().name()}</td>
                                    <td>${member.authorTasksCount()}</td>
                                    <td>${member.assigneeTasksCount()}</td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                @endif
            @endif
        </section>
    `
)