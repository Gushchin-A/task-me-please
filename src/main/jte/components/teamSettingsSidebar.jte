@import dev.gushchin.taskmanager.model.Team

@param Team team
@param String activeSection
@param boolean owner

<aside class="team-settings-sidebar">
    <nav aria-label="Настройки команды">
        @if(owner)
            <a
                class="${"general".equals(activeSection) ? "team-settings-nav-active" : ""}"
                href="/teams/${team.getId()}/settings"
                aria-current="${"general".equals(activeSection) ? "page" : null}"
            >
                Основные настройки
            </a>
        @endif

        <a
            class="${"members".equals(activeSection) ? "team-settings-nav-active" : ""}"
            href="/teams/${team.getId()}/members"
            aria-current="${"members".equals(activeSection) ? "page" : null}"
        >
            Участники
        </a>
        @if(owner)
            <a
                class="${"invitations".equals(activeSection) ? "team-settings-nav-active" : ""}"
                href="/teams/${team.getId()}/invite"
                aria-current="${"invitations".equals(activeSection) ? "page" : null}"
            >
                Приглашения
            </a>
            <a
                class="${"delete".equals(activeSection) ? "team-settings-nav-active" : ""}"
                href="/teams/${team.getId()}/settings?section=delete"
                aria-current="${"delete".equals(activeSection) ? "page" : null}"
            >
                Удаление команды
            </a>
        @else
            <a
                class="${"leave".equals(activeSection) ? "team-settings-nav-active" : ""}"
                href="/teams/${team.getId()}/settings?section=leave"
                aria-current="${"leave".equals(activeSection) ? "page" : null}"
            >
                Покинуть команду
            </a>
        @endif
    </nav>
</aside>
