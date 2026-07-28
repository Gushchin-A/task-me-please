@import org.springframework.security.web.csrf.CsrfToken
@import dev.gushchin.taskmanager.view.UserAvatarView

@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Создать команду",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "teams",
    content = @`
        <section>
            <h1>Создать команду</h1>

            <form method="post" action="/teams" id="create-team-form">
                <input
                    type="hidden"
                    name="${_csrf.getParameterName()}"
                    value="${_csrf.getToken()}"
                >

                <div>
                    <label>Название команды</label>

                    <input
                        type="text"
                        name="name"
                        required
                    >
                </div>

                <div style="margin-top: 16px;">
                    <label for="tag-name">
                        Теги команды
                    </label>

                    <div>
                        <input
                            id="tag-name"
                            type="text"
                            maxlength="30"
                            placeholder="Например, Кинопоиск"
                        >

                        <button
                            type="button"
                            onclick="addTeamTag()"
                        >
                            Добавить
                        </button>
                    </div>

                    <p>
                        Можно добавить до 20 тегов.
                        Максимальная длина одного тега — 30 символов.
                    </p>

                    <div id="team-tags"></div>
                </div>

                <button type="submit">
                    Создать
                </button>
            </form>
        </section>

        <script>
            const teamTags = [];

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
                    showFlashMessage('Такой тег уже добавлен.');
                    return;
                }

                if (teamTags.length >= 20) {
                    showFlashMessage(
                        'В команде может быть не больше 20 тегов.'
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

                    wrapper.style.marginTop = '8px';

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
                    removeButton.style.marginLeft = '8px';

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
