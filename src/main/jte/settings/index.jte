@import dev.gushchin.taskmanager.model.User
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param User user
@param String settingsSection
@param boolean hasOwnedTeams
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String successMessage = null
@param String errorMessage = null

@template.layout.page(
    title = "Настройки аккаунта",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = null,
    headerContext = null,
    successMessage = successMessage,
    errorMessage = errorMessage,
    content = @`
        <section class="workspace-page team-settings-page">
            @template.components.accountSettingsSidebar(activeSection = settingsSection)

            <div class="team-settings-content">
                @if("delete".equals(settingsSection))
                    <header class="team-create-header team-settings-header">
                        <h1>Удаление аккаунта</h1>
                        <p>
                            После удаления аккаунта вы потеряете доступ к сервису и вашим задачам.
                            Пожалуйста, будьте внимательны.
                        </p>
                    </header>

                    <section class="team-settings-delete-section">
                        <button class="button team-settings-delete" type="button" data-account-delete-open>
                            Удалить аккаунт
                        </button>
                    </section>
                @else
                    <header class="team-create-header team-settings-header">
                        <h1>Настройки профиля</h1>
                    </header>

                    <form
                        class="team-settings-form"
                        method="post"
                        action="/settings/name"
                        data-settings-rename-form
                        data-submit-loading
                    >
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                        <div class="form-field">
                            <label for="account-name">Имя</label>

                            <div class="field-with-action">
                                <input
                                    id="account-name"
                                    type="text"
                                    name="name"
                                    value="${user.getName()}"
                                    maxlength="255"
                                    autocomplete="name"
                                    aria-describedby="account-name-count"
                                    data-settings-name
                                    data-original-value="${user.getName()}"
                                >

                                <span class="team-settings-action-tooltip" data-tooltip="Начните редактировать">
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

                            <p
                                class="form-help task-title-count"
                                id="account-name-count"
                                hidden
                                data-settings-name-count
                            >
                                <span data-settings-name-length>${user.getName().length()}</span> / 255 символов
                            </p>
                        </div>
                    </form>
                @endif
            </div>
        </section>

        @if("delete".equals(settingsSection))
            <dialog class="settings-dialog" data-account-delete-dialog>
                <form
                    class="settings-dialog-form"
                    method="post"
                    action="/settings/delete"
                    data-submit-loading
                >
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <header class="settings-dialog-header">
                    <h2>Удаление аккаунта</h2>
                    <button type="button" aria-label="Закрыть" data-account-delete-close>×</button>
                </header>

                <div class="settings-dialog-body">
                    <p>
                        @if(hasOwnedTeams)
                            Вы уверены, что хотите удалить аккаунт? Кроме аккаунта будут также удалены все команды,
                            где вы владелец. Если да, напишите
                            <strong>«я хочу удалить аккаунт и команды»</strong> в поле ниже.
                        @else
                            Вы уверены, что хотите удалить аккаунт? Если да, напишите
                            <strong>«я хочу удалить аккаунт»</strong> в поле ниже.
                        @endif
                    </p>

                    <div class="form-field">
                        <label for="account-delete-confirmation">Введите проверочный текст</label>
                        <input
                            id="account-delete-confirmation"
                            type="text"
                            name="confirmationText"
                            required
                            autocomplete="off"
                            data-account-delete-input
                            data-confirmation-text="${hasOwnedTeams
                                ? "я хочу удалить аккаунт и команды"
                                : "я хочу удалить аккаунт"}"
                        >
                    </div>
                </div>

                <footer class="settings-dialog-footer">
                    <button
                        class="button button-danger"
                        type="submit"
                        disabled
                        data-account-delete-submit
                        data-loading-text="Обработка..."
                    >
                        Удалить
                    </button>
                </footer>
                </form>
            </dialog>
        @endif
    `
)
