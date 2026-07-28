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
        <details class="team-switcher">
            <summary data-tooltip="Выбрать команду">
                <span>${team.getName()}</span>
                <span aria-hidden="true">⌄</span>
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
            </div>
        </details>

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
        </nav>

        @if(canInvite)
            <a class="button button-secondary team-invite-action" href="/teams/${team.getId()}/invite">
                Пригласить
            </a>
        @else
            <span class="team-invite-disabled" data-tooltip="Только owner команды может приглашать новых участников">
                <button class="button button-secondary team-invite-action" type="button" disabled>
                    Пригласить
                </button>
            </span>
        @endif

        <span class="team-settings-wrapper" data-tooltip="Настройки пока не реализованы">
            <button class="button button-secondary team-settings-action" type="button" disabled>Настройки</button>
        </span>
    </div>
</section>
