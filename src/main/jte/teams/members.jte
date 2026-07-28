@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamMemberRole
@import dev.gushchin.taskmanager.model.TeamTaskVisibility
@import dev.gushchin.taskmanager.view.TeamMemberView
@import dev.gushchin.taskmanager.view.UserAvatarView
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
@param String successMessage
@param String errorMessage
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — участники",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    content = @`
        @template.components.teamNavigation(
            team = team,
            activeTab = "members",
            tasksCount = totalTasksCount,
            membersCount = membersCount,
            canInvite = canInvite
        )

        <section class="workspace-page team-members-page">
            <header class="page-header page-header-compact">
                <div>
                    <h1>Участники</h1>
                    <p>Участники и доступ к задачам команды.</p>
                </div>
            </header>

            <div class="flash-stack" aria-live="polite" aria-atomic="false">
                @if(successMessage != null)
                    @template.components.flash(message = successMessage, type = "success")
                @endif

                @if(errorMessage != null)
                    @template.components.flash(message = errorMessage, type = "error")
                @endif
            </div>

            @if(onlyCurrentOwnerInTeam)
                <div class="empty-state empty-state-compact">
                    <p>Кроме вас в команде пока никого нет.</p>

                    @if(canInvite)
                        <a href="/teams/${team.getId()}/invite">
                            Пригласить в команду
                        </a>
                    @else
                        <span class="disabled-action">Приглашать участников может только owner команды</span>
                    @endif
                </div>
            @endif

            @if(members.isEmpty())
                    <div class="empty-state empty-state-compact">
                        <p>В команде нет участников.</p>

                        @if(canInvite)
                            <a href="/teams/${team.getId()}/invite">
                                Пригласить в команду
                            </a>
                        @else
                            <span class="disabled-action">Приглашать участников может только owner команды</span>
                        @endif
                    </div>
                @else
                    <div class="table-card">
                    <table class="data-table">
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
                                    <th>Действия</th>
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

                                        <td>
                                            @if(member.role() == TeamMemberRole.MEMBER && !member.userId().equals(currentUserId))
                                                <details>
                                                    <summary>Удалить</summary>

                                                    <form
                                                        method="post"
                                                        action="/teams/${team.getId()}/members/${member.userId().toString()}/remove"
                                                        style="display: inline;"
                                                    >
                                                        <input
                                                            type="hidden"
                                                            name="${_csrf.getParameterName()}"
                                                            value="${_csrf.getToken()}"
                                                        >

                                                        <span>Удалить участника из команды?</span>

                                                        <button type="submit">
                                                            Да
                                                        </button>

                                                        <button
                                                            type="button"
                                                            onclick="this.closest('details').removeAttribute('open')"
                                                        >
                                                            Нет
                                                        </button>
                                                    </form>
                                                </details>
                                            @endif
                                        </td>
                                    @endif
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                    </div>
            @endif
        </section>
    `
)
