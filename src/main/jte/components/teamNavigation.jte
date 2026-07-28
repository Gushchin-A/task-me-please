@import dev.gushchin.taskmanager.model.Team

@param Team team
@param String activeTab
@param int tasksCount
@param int membersCount
@param boolean canInvite

<section class="team-context" aria-label="Текущая команда">
    <div class="team-context-inner">
        <div class="team-identity">
            <a class="team-list-link" href="/teams">Все команды</a>
            <span aria-hidden="true">/</span>
            <a class="team-name-link" href="/teams/${team.getId()}">${team.getName()}</a>
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
                Архив
            </a>
        </nav>

        @if(canInvite)
            <a class="button button-secondary team-invite-action" href="/teams/${team.getId()}/invite">
                Пригласить
            </a>
        @endif
    </div>
</section>
