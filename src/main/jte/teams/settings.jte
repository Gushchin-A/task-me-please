@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.TeamTag
@import dev.gushchin.taskmanager.view.UserAvatarView
@import java.util.List
@import java.util.Set
@import org.springframework.security.web.csrf.CsrfToken

@param Team team
@param List<Team> navigationTeams
@param List<TeamTag> tags
@param Set<Long> usedTagIds
@param String settingsSection
@param String successMessage
@param String errorMessage
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = team.getName() + " — настройки",
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

        <section class="workspace-page team-settings-page">
            <aside class="team-settings-sidebar">
                <nav aria-label="Настройки команды">
                    <a
                        class="${"general".equals(settingsSection) ? "team-settings-nav-active" : ""}"
                        href="/teams/${team.getId()}/settings"
                        aria-current="${"general".equals(settingsSection) ? "page" : null}"
                    >
                        Основные настройки
                    </a>
                    <a href="/teams/${team.getId()}/members">Участники</a>
                    <a
                        class="${"delete".equals(settingsSection) ? "team-settings-nav-active" : ""}"
                        href="/teams/${team.getId()}/settings?section=delete"
                        aria-current="${"delete".equals(settingsSection) ? "page" : null}"
                    >
                        Удаление команды
                    </a>
                </nav>
            </aside>

            <div class="team-settings-content">
                @if("delete".equals(settingsSection))
                    <header class="team-create-header team-settings-header">
                        <h1>Удаление команды</h1>
                    </header>

                    <section class="team-settings-delete-section">
                        <p>
                            После удаления команды она и все ее задачи исчезнут навсегда.
                            Пожалуйста, будьте внимательны.
                        </p>

                        <a class="button team-settings-delete" href="/teams/${team.getId()}/delete">
                            Удалить команду
                        </a>
                    </section>
                @else
                    <header class="team-create-header team-settings-header">
                        <h1>Основные настройки</h1>
                    </header>

                    <form
                        class="team-settings-form"
                        method="post"
                        action="/teams/${team.getId()}/settings/name"
                        data-settings-rename-form
                        data-submit-loading
                    >
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                        <div class="form-field">
                            <label for="team-name">Название команды</label>

                            <div class="field-with-action">
                                <input
                                    id="team-name"
                                    type="text"
                                    name="name"
                                    value="${team.getName()}"
                                    maxlength="100"
                                    required
                                    autocomplete="off"
                                    aria-describedby="team-name-count"
                                    data-settings-name
                                    data-original-value="${team.getName()}"
                                >

                                <span class="team-settings-action-tooltip" data-tooltip="Начните редактировать название">
                                    <button
                                        class="button button-secondary"
                                        type="submit"
                                        disabled
                                        data-settings-rename-submit
                                        data-loading-text="Обработка..."
                                    >
                                        Изменить
                                    </button>
                                </span>
                            </div>

                            <p class="form-help task-title-count" id="team-name-count" hidden data-settings-name-count>
                                <span data-settings-name-length>${team.getName().length()}</span> / 100 символов
                            </p>
                        </div>
                    </form>

                    <form
                        class="team-settings-form team-settings-tags-form"
                        method="post"
                        action="/teams/${team.getId()}/settings/tags"
                    >
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                        <div class="form-field">
                            <label for="tag-name">Теги</label>

                            <div class="field-with-action">
                                <input
                                    id="tag-name"
                                    type="text"
                                    name="name"
                                    maxlength="30"
                                    placeholder="Например, Кинопоиск"
                                    required
                                >

                                <button class="button button-secondary" type="submit">
                                    Добавить
                                </button>
                            </div>

                            <p class="form-help">
                                Длина одного тега до 30 символов. Всего можно добавить до 20 тегов.
                            </p>
                        </div>
                    </form>

                    <div class="tag-list team-settings-tag-list">
                        @for(TeamTag tag : tags)
                            <span class="tag-chip">
                                <span>${tag.getName()}</span>
                                @if(usedTagIds.contains(tag.getId()))
                                    <button
                                        class="tag-chip-remove"
                                        type="button"
                                        aria-label="Изменить тег ${tag.getName()}"
                                        data-tag-rename-open
                                        data-tag-id="${tag.getId()}"
                                        data-tag-name="${tag.getName()}"
                                    >
                                        ×
                                    </button>
                                @else
                                    <form
                                        method="post"
                                        action="/teams/${team.getId()}/settings/tags/${tag.getId()}/delete"
                                    >
                                        <input
                                            type="hidden"
                                            name="${_csrf.getParameterName()}"
                                            value="${_csrf.getToken()}"
                                        >
                                        <button
                                            class="tag-chip-remove"
                                            type="submit"
                                            aria-label="Удалить тег ${tag.getName()}"
                                        >
                                            ×
                                        </button>
                                    </form>
                                @endif
                            </span>
                        @endfor
                    </div>
                @endif
            </div>
        </section>

        @if("general".equals(settingsSection))
            <dialog class="settings-dialog" data-tag-rename-dialog>
                <form class="settings-dialog-form" method="post" data-tag-rename-form data-submit-loading>
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <header class="settings-dialog-header">
                    <h2>Удаление тега</h2>
                    <button type="button" aria-label="Закрыть" data-tag-rename-close>×</button>
                </header>

                <div class="settings-dialog-body">
                    <p>
                        Тег, уже присвоенный задачам, удалить невозможно, но его можно изменить.
                        Измененный тег появится у всех задач вместо старого.
                    </p>

                    <div class="form-field">
                        <label for="renamed-tag-name">Введите новое название</label>
                        <input
                            id="renamed-tag-name"
                            type="text"
                            name="name"
                            maxlength="30"
                            required
                            autocomplete="off"
                            data-tag-rename-input
                        >
                    </div>
                </div>

                <footer class="settings-dialog-footer">
                    <button
                        class="button button-primary"
                        type="submit"
                        disabled
                        data-tag-rename-submit
                        data-loading-text="Обработка..."
                    >
                        Изменить
                    </button>
                </footer>
                </form>
            </dialog>
        @endif
    `
)
