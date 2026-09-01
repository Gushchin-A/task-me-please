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
@param boolean owner
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
            @template.components.teamSettingsSidebar(
                team = team,
                activeSection = settingsSection,
                owner = owner
            )

            <div class="team-settings-content">
                @if("leave".equals(settingsSection))
                    <header class="team-create-header team-settings-header">
                        <h1>Покинуть команду</h1>
                        <p>
                            После выхода из команды все ее задачи, включая ваши, будут недоступны.
                            Пожалуйста, будьте внимательны.
                        </p>
                    </header>

                    <section class="team-settings-delete-section">
                        <button class="button team-settings-delete" type="button" data-team-leave-open>
                            Покинуть команду
                        </button>
                    </section>
                @elseif("delete".equals(settingsSection))
                    <header class="team-create-header team-settings-header">
                        <h1>Удаление команды</h1>
                        <p>
                            После удаления команды она и все ее задачи исчезнут навсегда.
                            Пожалуйста, будьте внимательны.
                        </p>
                    </header>

                    <section class="team-settings-delete-section">
                        <button class="button team-settings-delete" type="button" data-team-delete-open>
                            Удалить команду
                        </button>
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
        @elseif("delete".equals(settingsSection))
            <dialog class="settings-dialog" data-team-delete-dialog>
                <form
                    class="settings-dialog-form"
                    method="post"
                    action="/teams/${team.getId()}/delete"
                    data-submit-loading
                    data-pointer-submit-only
                >
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <header class="settings-dialog-header">
                    <h2>Удаление команды</h2>
                    <button type="button" aria-label="Закрыть" data-team-delete-close>×</button>
                </header>

                <div class="settings-dialog-body">
                    <p>
                        Вы уверены, что хотите удалить команду «${team.getName()}»?
                        Если да, напишите <strong>«я хочу удалить команду»</strong> в поле ниже.
                    </p>

                    <div class="form-field">
                        <label for="team-delete-confirmation">Введите проверочный текст</label>
                        <input
                            id="team-delete-confirmation"
                            type="text"
                            name="confirmationText"
                            required
                            autocomplete="off"
                            data-team-delete-input
                            data-confirmation-text="я хочу удалить команду"
                        >
                    </div>
                </div>

                <footer class="settings-dialog-footer">
                    <button
                        class="button button-danger"
                        type="submit"
                        disabled
                        tabindex="-1"
                        data-team-delete-submit
                        data-pointer-submit
                        data-loading-text="Обработка..."
                    >
                        Удалить
                    </button>
                </footer>
                </form>
            </dialog>
        @elseif("leave".equals(settingsSection))
            <dialog class="settings-dialog" data-team-leave-dialog>
                <form
                    class="settings-dialog-form"
                    method="post"
                    action="/teams/${team.getId()}/leave"
                    data-submit-loading
                >
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <header class="settings-dialog-header">
                    <h2>Покинуть команду</h2>
                    <button type="button" aria-label="Закрыть" data-team-leave-close>×</button>
                </header>

                <div class="settings-dialog-body">
                    <p>
                        Вы уверены, что хотите покинуть команду «${team.getName()}»?
                        Если да, напишите <strong>«да хочу выйти из команды»</strong> в поле ниже.
                    </p>

                    <div class="form-field">
                        <label for="team-leave-confirmation">Введите проверочный текст</label>
                        <input
                            id="team-leave-confirmation"
                            type="text"
                            name="confirmationText"
                            required
                            autocomplete="off"
                            data-team-leave-input
                            data-confirmation-text="да хочу выйти из команды"
                        >
                    </div>
                </div>

                <footer class="settings-dialog-footer">
                    <button
                        class="button button-danger"
                        type="submit"
                        disabled
                        data-team-leave-submit
                        data-loading-text="Обработка..."
                    >
                        Покинуть команду
                    </button>
                </footer>
                </form>
            </dialog>
        @endif
    `
)
