@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.view.MyTasksPageView
@import dev.gushchin.taskmanager.view.UserAvatarView
@import dev.gushchin.taskmanager.view.TaskWithTeamView
@import org.springframework.security.web.csrf.CsrfToken

@param MyTasksPageView page
@param CsrfToken _csrf
@param UserAvatarView currentUser

@template.layout.page(
    title = "Мои задачи",
    _csrf = _csrf,
    currentUser = currentUser,
    activeSection = "tasks",
    headerContext = null,
    content = @`
        <section class="workspace-page tasks-page">
            @if(page.showTaskWorkspaceNavigation())
                <nav class="local-tabs" aria-label="Режим списка задач">
                <a
                    class="${page.activeMode() ? "local-tab-active" : ""}"
                    href="/tasks"
                    aria-current="${page.activeMode() ? "page" : null}"
                >
                    Задачи
                </a>

                <a
                    class="${page.archiveMode() ? "local-tab-active" : ""}"
                    href="/tasks/archive"
                    aria-current="${page.archiveMode() ? "page" : null}"
                >
                    Архив
                </a>
                </nav>

                @template.components.myTasksToolbar(page = page)
            @else
                <div class="page-actions tasks-onboarding-actions">
                    @if(page.showTeamPrerequisiteState())
                        <a class="button button-primary team-create-action" href="/teams/new">
                            Создать команду
                        </a>
                    @else
                        <a class="button button-primary task-create-action" href="/tasks/new">
                            Создать задачу
                        </a>
                    @endif
                </div>
            @endif

            @if(page.tasks().isEmpty())
                <div class="empty-state task-empty-state">
                    @if(page.showTeamPrerequisiteState())
                        <div class="empty-state-icons" aria-hidden="true">
                            <svg
                                class="empty-state-group-icon"
                                focusable="false"
                                viewBox="0 0 24 24"
                                width="24"
                                height="24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="1.5"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            >
                                <path d="M7.5 5.25H5.75A2.75 2.75 0 0 0 3 8v11.25A1.75 1.75 0 0 0 4.75 21h14.5A1.75 1.75 0 0 0 21 19.25V8a2.75 2.75 0 0 0-2.75-2.75H16.5"></path>
                                <path d="M7.5 2.75v4M16.5 2.75v4"></path>
                                <path d="M7.5 10h7M7.5 13.5h5"></path>
                                <path d="m14 17 1.75 1.75 3.25-4.5"></path>
                            </svg>

                            <svg
                                class="empty-state-group-icon"
                                focusable="false"
                                viewBox="0 0 24 24"
                                width="24"
                                height="24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="1.5"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            >
                                <circle cx="12" cy="7" r="3.25"></circle>
                                <circle cx="5.25" cy="8.25" r="2.25"></circle>
                                <circle cx="18.75" cy="8.25" r="2.25"></circle>
                                <path d="M6.5 20.25H3.25v-3.5a4.25 4.25 0 0 1 5.25-4.13"></path>
                                <path d="M17.5 20.25h3.25v-3.5a4.25 4.25 0 0 0-5.25-4.13"></path>
                                <path d="M7.25 20.25v-4.5a4.75 4.75 0 0 1 9.5 0v4.5Z"></path>
                            </svg>
                        </div>

                        <h2>Актуальные задачи и команды не найдены</h2>
                        <p>Чтобы работать с задачами, создайте команду или попросите добавить вас в существующую</p>
                    @else
                        <svg
                            class="empty-state-icon"
                            aria-hidden="true"
                            focusable="false"
                            viewBox="0 0 24 24"
                            width="24"
                            height="24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        >
                            <path d="M7.5 5.25H5.75A2.75 2.75 0 0 0 3 8v11.25A1.75 1.75 0 0 0 4.75 21h14.5A1.75 1.75 0 0 0 21 19.25V8a2.75 2.75 0 0 0-2.75-2.75H16.5"></path>
                            <path d="M7.5 2.75v4M16.5 2.75v4"></path>
                            <path d="M7.5 10h7M7.5 13.5h5"></path>
                            <path d="m14 17 1.75 1.75 3.25-4.5"></path>
                        </svg>

                        @if(page.archiveMode())
                            <h2>В архиве пока что пусто</h2>
                            <p>Сюда вы сможете перенести выполненные, неактуальные или удаленные задачи</p>
                        @else
                            <h2>Задачи не найдены</h2>
                            <p>Измените фильтры или создайте новую задачу</p>
                        @endif
                    @endif
                </div>
            @else
                <ul class="task-grid">
                    @for(TaskWithTeamView taskCard : page.tasks())
                        @template.components.taskCard(
                            task = taskCard.task(),
                            teamId = taskCard.teamId(),
                            teamName = taskCard.teamName(),
                            members = taskCard.members(),
                            tags = taskCard.tags(),
                            returnTo = page.archiveMode() ? "tasksArchive" : "tasks",
                            _csrf = _csrf
                        )
                    @endfor
                </ul>
            @endif
        </section>
    `
)
