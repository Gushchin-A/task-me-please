@import dev.gushchin.taskmanager.model.Team
@import java.util.List

@param Team team
@param List<Team> teams
@param String activeTab
@param int tasksCount
@param int archiveCount
@param int membersCount
@param boolean canInvite

<section class="team-context" aria-label="Текущая команда">
    <div class="team-context-inner">
        <div class="team-identity">
            <a class="team-name-link" href="/teams/${team.getId()}">${team.getName()}</a>

            <details class="team-switcher">
                <summary data-tooltip="Выбрать команду" aria-label="Выбрать команду">
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16">
                        <path d="M8 11 3 6l.7-.7L8 9.6l4.3-4.3.7.7-5 5Z" fill="currentColor"></path>
                    </svg>
                </summary>

                <div class="team-switcher-menu">
                    @for(Team availableTeam : teams)
                        <a
                            href="/teams/${availableTeam.getId()}"
                            aria-current="${availableTeam.getId().equals(team.getId()) ? "page" : null}"
                        >
                            ${availableTeam.getName()}
                        </a>
                    @endfor

                    <a class="team-switcher-all" href="/teams">Все команды</a>
                </div>
            </details>
        </div>

        <nav class="team-tabs" aria-label="Разделы команды">
            <a
                class="team-tab ${"tasks".equals(activeTab) ? "team-tab-active" : ""}"
                href="/teams/${team.getId()}"
                aria-current="${"tasks".equals(activeTab) ? "page" : null}"
            >
                Задачи@if(tasksCount >= 0)<span class="nav-count">${tasksCount}</span>@endif
            </a>

            <a
                class="team-tab ${"members".equals(activeTab) ? "team-tab-active" : ""}"
                href="/teams/${team.getId()}/members"
                aria-current="${"members".equals(activeTab) ? "page" : null}"
            >
                Участники@if(membersCount >= 0)<span class="nav-count">${membersCount}</span>@endif
            </a>

            <a
                class="team-tab ${"archive".equals(activeTab) ? "team-tab-active" : ""}"
                href="/teams/${team.getId()}/archive"
                aria-current="${"archive".equals(activeTab) ? "page" : null}"
            >
                Архив@if(archiveCount >= 0)<span class="nav-count">${archiveCount}</span>@endif
            </a>

            <span
                class="team-tab team-settings-action"
                data-tooltip="Настройки пока не реализованы"
                tabindex="0"
                aria-disabled="true"
            >
                Настройки
            </span>
        </nav>

        @if(canInvite)
            <a class="button button-secondary team-invite-action" href="/teams/${team.getId()}/invite">
                Пригласить
            </a>
        @else
            <span
                class="team-invite-disabled"
                data-tooltip="Только owner команды может приглашать новых участников"
                tabindex="0"
            >
                <button class="button button-secondary team-invite-action" type="button" disabled>
                    Пригласить
                </button>
            </span>
        @endif

    </div>
</section>
