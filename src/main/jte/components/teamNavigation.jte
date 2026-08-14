@import dev.gushchin.taskmanager.model.Team

@param Team team
@param String activeTab

<nav class="local-tabs team-tabs" aria-label="Разделы команды">
    <a
        class="${"tasks".equals(activeTab) ? "local-tab-active" : ""}"
        href="/teams/${team.getId()}"
        aria-current="${"tasks".equals(activeTab) ? "page" : null}"
    >
        Задачи
    </a>

    <a
        class="${"archive".equals(activeTab) ? "local-tab-active" : ""}"
        href="/teams/${team.getId()}/archive"
        aria-current="${"archive".equals(activeTab) ? "page" : null}"
    >
        Архив
    </a>

    <span
        class="local-tab-disabled team-settings-action"
        data-tooltip="Настройки пока не реализованы"
        tabindex="0"
        aria-disabled="true"
    >
        Настройки
    </span>
</nav>
