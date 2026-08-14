@import dev.gushchin.taskmanager.model.Team
@import java.util.List

@param Team team
@param List<Team> navigationTeams

<div class="app-team-context">
    <a class="app-team-link" href="/teams/${team.getId()}">${team.getName()}</a>

    <details class="team-switcher">
        <summary
            class="team-switcher-trigger"
            data-tooltip="Сменить команду"
            aria-label="Сменить команду"
            aria-haspopup="menu"
        >
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                <path d="m4.427 7.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 7H4.604a.25.25 0 0 0-.177.427Z"></path>
            </svg>
        </summary>

        <div class="team-switcher-menu" role="menu">
            <div class="team-switcher-heading">
                <span>Сменить команду</span>
                <button
                    class="team-switcher-close"
                    type="button"
                    aria-label="Закрыть список команд"
                    data-team-switcher-close
                >
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                        <path d="M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z"></path>
                    </svg>
                </button>
            </div>

            <div class="team-switcher-list">
                @for(Team navigationTeam : navigationTeams)
                    <a
                        class="team-switcher-item"
                        href="/teams/${navigationTeam.getId()}"
                        role="menuitem"
                        aria-current="${navigationTeam.getId().equals(team.getId()) ? "page" : null}"
                    >
                        <svg
                            class="team-switcher-check"
                            aria-hidden="true"
                            width="16"
                            height="16"
                            viewBox="0 0 16 16"
                            fill="currentColor"
                        >
                            @if(navigationTeam.getId().equals(team.getId()))
                                <path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.751.751 0 0 1 .018-1.042.751.751 0 0 1 1.042-.018L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path>
                            @endif
                        </svg>
                        <span>${navigationTeam.getName()}</span>
                    </a>
                @endfor
            </div>

            <a class="team-switcher-all" href="/teams">Все команды</a>
        </div>
    </details>
</div>
