@import org.springframework.security.web.csrf.CsrfToken
@import dev.gushchin.taskmanager.view.UserAvatarView

@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String errorMessage

@template.layout.page(
    title = "Создать команду",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    errorMessage = errorMessage,
    headerContext = null,
    content = @`
        <section class="workspace-page form-page team-create-page">
            <a class="back-link team-create-back-link" href="/teams">
                <svg
                    aria-hidden="true"
                    width="16"
                    height="16"
                    viewBox="0 0 16 16"
                    fill="none"
                >
                    <path
                        d="M6.75 3.25 2 8m0 0 4.75 4.75M2 8h12"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    ></path>
                </svg>
                <span>Вернуться назад</span>
            </a>

            <header class="page-header page-header-compact team-create-header">
                <div>
                    <h1>Создание новой команды</h1>
                    <p class="form-page-description">
                        Придумайте название и заполните теги. Теги помогут участникам быстрее
                        ориентироваться в задачах. Команду можно создать и без них.
                    </p>
                </div>
            </header>

            <form class="team-create-form" method="post" action="/teams" id="create-team-form">
                <input
                    type="hidden"
                    name="${_csrf.getParameterName()}"
                    value="${_csrf.getToken()}"
                >

                <div class="form-field">
                    <label for="team-name">
                        Название команды <span class="required-marker" aria-hidden="true">*</span>
                    </label>

                    <input
                        id="team-name"
                        type="text"
                        name="name"
                        maxlength="100"
                        required
                        aria-describedby="team-name-count"
                    >

                    <p class="form-help task-title-count" id="team-name-count">
                        <span data-length-output>0</span> / 100 символов
                    </p>
                </div>

                <div class="form-field">
                    <label for="tag-name">Теги</label>

                    <div class="field-with-action">
                        <input
                            id="tag-name"
                            type="text"
                            maxlength="30"
                            placeholder="Например, Кинопоиск"
                        >

                        <button
                            class="button button-secondary"
                            type="button"
                            onclick="addTeamTag()"
                        >
                            Добавить
                        </button>
                    </div>

                    <p class="form-help">
                        Длина одного тега до 30 символов. После создания команды их можно
                        редактировать и удалять в настройках. Всего можно добавить до 20 тегов.
                    </p>

                    <div class="tag-list" id="team-tags"></div>
                </div>

                <div class="team-create-submit">
                    <button class="button button-primary" type="submit">
                        Создать команду
                    </button>
                </div>
            </form>
        </section>

        <script>
            const teamTags = [];
            const teamNameInput = document.getElementById('team-name');
            const teamNameLength = document.querySelector('[data-length-output]');

            teamNameInput.addEventListener('input', function () {
                teamNameLength.textContent = teamNameInput.value.length;
            });

            function addTeamTag() {
                const input = document.getElementById('tag-name');
                const name = input.value.trim();

                if (name === '') {
                    return;
                }

                const duplicate = teamTags.some(function (tag) {
                    return tag.toLocaleLowerCase()
                        === name.toLocaleLowerCase();
                });

                if (duplicate) {
                    showFlashMessage('Такой тег уже добавлен');
                    return;
                }

                if (teamTags.length >= 20) {
                    showFlashMessage(
                        'В команде может быть не больше 20 тегов'
                    );
                    return;
                }

                teamTags.push(name);
                input.value = '';

                renderTeamTags();
            }

            function removeTeamTag(index) {
                teamTags.splice(index, 1);

                renderTeamTags();
            }

            function renderTeamTags() {
                const container =
                    document.getElementById('team-tags');

                container.innerHTML = '';

                teamTags.forEach(function (tag, index) {
                    const wrapper =
                        document.createElement('div');

                    wrapper.className = 'tag-chip';

                    const hiddenInput =
                        document.createElement('input');

                    hiddenInput.type = 'hidden';
                    hiddenInput.name = 'tags';
                    hiddenInput.value = tag;

                    const text =
                        document.createElement('span');

                    text.textContent = tag;

                    const removeButton =
                        document.createElement('button');

                    removeButton.type = 'button';
                    removeButton.textContent = '×';
                    removeButton.className = 'tag-chip-remove';
                    removeButton.setAttribute('aria-label', 'Удалить тег ' + tag);

                    removeButton.addEventListener(
                        'click',
                        function () {
                            removeTeamTag(index);
                        }
                    );

                    wrapper.appendChild(hiddenInput);
                    wrapper.appendChild(text);
                    wrapper.appendChild(removeButton);

                    container.appendChild(wrapper);
                });
            }

            document
                .getElementById('tag-name')
                .addEventListener('keydown', function (event) {
                    if (event.key === 'Enter') {
                        event.preventDefault();

                        addTeamTag();
                    }
                });
        </script>
    `
)
