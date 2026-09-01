@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamMemberRole
@import dev.gushchin.taskmanager.model.TeamTaskVisibility
@import dev.gushchin.taskmanager.view.TeamMemberView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import java.util.UUID
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param List<Team> navigationTeams
@param List<TeamMemberView> members
@param boolean canInvite
@param boolean canManageVisibility
@param UUID currentUserId
@param CsrfToken _csrf
@param String successMessage
@param String errorMessage
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — участники",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    successMessage = successMessage,
    errorMessage = errorMessage,
    headerContext = @`
        @template.components.teamSwitcher(team = team, navigationTeams = navigationTeams)
    `,
    content = @`
        @template.components.teamNavigation(team = team, activeTab = "settings")

        <section class="workspace-page team-settings-page team-members-settings-page">
            @template.components.teamSettingsSidebar(team = team, activeSection = "members", owner = canInvite)

            <div class="team-settings-content">
                <header class="team-create-header team-settings-header">
                    <h1>Участники</h1>
                    <p>${canManageVisibility
                        ? "Настройте, видит ли участник все задачи команды или только задачи, где он автор или исполнитель"
                        : "Актуальный список участников команды «" + team.getName() + "»"}</p>
                </header>

                <div class="${canManageVisibility ? "team-member-list" : "team-member-list team-member-list-read-only"}">
                    @for(TeamMemberView member : members)
                        <div class="${canManageVisibility ? "team-member-row" : "team-member-row team-member-row-read-only"}">
                            <div class="team-member-profile">
                                <span
                                    class="team-member-avatar"
                                    aria-hidden="true"
                                >
                                    ${member.initials()}
                                </span>
                                <div class="team-member-identity">
                                    <strong title="${member.name()}">${member.displayName()}</strong>
                                    <span>${member.email()}</span>
                                </div>
                            </div>

                            <div class="team-member-role-cell">
                                @if(member.role() == TeamMemberRole.OWNER)
                                    <span class="team-member-role task-card-deadline">Владелец</span>
                                @elseif(!canManageVisibility
                                    && member.userId().equals(currentUserId)
                                    && member.taskVisibility() == TeamTaskVisibility.ALL_TASKS)
                                    <span class="team-member-role task-card-deadline">Видит все задачи</span>
                                @endif
                            </div>

                            @if(canManageVisibility)
                                <div class="team-member-visibility">
                                @if(member.role() != TeamMemberRole.OWNER)
                                    <form
                                        class="team-member-visibility-control"
                                        method="post"
                                        action="/teams/${team.getId()}/members/${member.userId().toString()}/visibility"
                                        data-team-visibility-form
                                    >
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                        <input
                                            type="hidden"
                                            name="taskVisibility"
                                            value="${member.taskVisibility() == TeamTaskVisibility.ALL_TASKS
                                                ? TeamTaskVisibility.OWN_TASKS.name()
                                                : TeamTaskVisibility.ALL_TASKS.name()}"
                                        >
                                        <button
                                            class="team-visibility-switch"
                                            type="submit"
                                            role="switch"
                                            aria-checked="${member.taskVisibility() == TeamTaskVisibility.ALL_TASKS}"
                                            aria-label="Изменить видимость задач для ${member.name()}"
                                        ><span aria-hidden="true"></span></button>
                                    </form>
                                @else
                                    <div class="team-member-visibility-control">
                                        <button
                                            class="team-visibility-switch"
                                            type="button"
                                            role="switch"
                                            aria-checked="${member.taskVisibility() == TeamTaskVisibility.ALL_TASKS}"
                                            aria-disabled="true"
                                            disabled
                                        ><span aria-hidden="true"></span></button>
                                    </div>
                                @endif
                                <span data-team-visibility-status>${member.taskVisibility() == TeamTaskVisibility.ALL_TASKS
                                    ? "Видит все задачи" : "Видит только свои задачи"}</span>
                                </div>

                                <div class="team-member-remove-cell">
                                @if(member.role() != TeamMemberRole.OWNER)
                                    <button
                                        class="task-detail-icon-button"
                                        type="button"
                                        aria-label="Удалить ${member.name()} из команды"
                                        data-tooltip="Удалить участника"
                                        data-team-member-remove-open
                                        data-member-id="${member.userId().toString()}"
                                        data-member-name="${member.name()}"
                                        data-member-email="${member.email()}"
                                        data-member-initials="${member.initials()}"
                                    >
                                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <path d="M6.5 1.75A.75.75 0 0 1 7.25 1h1.5a.75.75 0 0 1 .75.75V3h3.75a.75.75 0 0 1 0 1.5h-.55l-.66 9.19A1.42 1.42 0 0 1 10.62 15H5.38a1.42 1.42 0 0 1-1.42-1.31L3.3 4.5h-.55a.75.75 0 0 1 0-1.5H6.5V1.75ZM4.8 4.5l.65 9h5.1l.65-9H4.8Z"></path>
                                        </svg>
                                    </button>
                                @endif
                                </div>
                            @endif
                        </div>
                    @endfor
                </div>

                @if(canManageVisibility)
                    <dialog class="settings-dialog team-member-remove-dialog" data-team-member-remove-dialog>
                        <form class="settings-dialog-form" method="post" data-team-member-remove-form data-submit-loading>
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <header class="settings-dialog-header">
                                <h2>Удаление участника</h2>
                                <button
                                    class="settings-dialog-close"
                                    type="button"
                                    aria-label="Закрыть"
                                    data-team-member-remove-close
                                >
                                    ×
                                </button>
                            </header>

                            <div class="settings-dialog-body">
                                <div class="team-member-remove-profile">
                                    <span class="team-member-avatar" aria-hidden="true" data-team-member-remove-avatar></span>
                                    <div class="team-member-identity">
                                        <strong data-team-member-remove-name></strong>
                                        <span data-team-member-remove-email></span>
                                    </div>
                                </div>
                                <p>Участник потеряет доступ к задачам команды. Его можно будет пригласить повторно.</p>
                            </div>

                            <footer class="settings-dialog-footer">
                                <button class="button button-secondary" type="button" data-team-member-remove-cancel>
                                    Отмена
                                </button>
                                <button class="button button-danger" type="submit" data-loading-text="Обработка...">
                                    Удалить
                                </button>
                            </footer>
                        </form>
                    </dialog>
                @endif
            </div>
        </section>
    `
)
