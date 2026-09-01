@param String activeSection

<aside class="team-settings-sidebar">
    <nav aria-label="Настройки аккаунта">
        <a
            class="${"profile".equals(activeSection) ? "team-settings-nav-active" : ""}"
            href="/settings"
            aria-current="${"profile".equals(activeSection) ? "page" : null}"
        >
            Настройки профиля
        </a>

        <a
            class="${"delete".equals(activeSection) ? "team-settings-nav-active" : ""}"
            href="/settings?section=delete"
            aria-current="${"delete".equals(activeSection) ? "page" : null}"
        >
            Удаление аккаунта
        </a>
    </nav>
</aside>
