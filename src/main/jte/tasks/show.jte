@import dev.gushchin.taskmanager.model.TaskCategory
@import dev.gushchin.taskmanager.model.TaskStatus
@import dev.gushchin.taskmanager.model.Team
@import dev.gushchin.taskmanager.model.User
@import dev.gushchin.taskmanager.view.CommentView
@import dev.gushchin.taskmanager.view.TaskView
@import java.util.List
@import org.springframework.security.web.csrf.CsrfToken

@param TaskView task
@param Team team
@param List<User> members
@param TaskCategory[] categories
@param List<CommentView> comments
@param CsrfToken _csrf

@template.layout.page(
    title = task.title(),
    content = @`
        <section>
            <a href="/teams/${team.getId()}">
                ← Назад к команде
            </a>

            <div>
                <h1 style="display: inline;">
                    ${task.title()}
                </h1>

                @if(task.canUpdateTask())
                    <a
                        href="#"
                        style="margin-left: 12px; font-size: 14px;"
                        onclick="document.getElementById('title-edit-form').style.display = 'block'; this.style.display = 'none'; return false;"
                    >
                        Редактировать название
                    </a>
                @endif
            </div>

            @if(task.canUpdateTask())
                <form
                    id="title-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    style="display: none; margin-top: 12px;"
                >
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                    <input type="hidden" name="category" value="${task.category().name()}">
                    <input type="hidden" name="assigneeId" value="${task.assigneeId().toString()}">

                    @if(task.deadlineAt() != null)
                        <input
                            type="hidden"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif

                    <textarea name="description" style="display: none;">@if(task.description() != null)${task.description()}@endif</textarea>

                    <div>
                        <label>Название</label>

                        <input type="text" name="title" value="${task.title()}" required>
                    </div>

                    <button type="submit">
                        Сохранить название
                    </button>

                    <button
                        type="button"
                        onclick="document.getElementById('title-edit-form').style.display = 'none';"
                    >
                        Отмена
                    </button>
                </form>
            @endif

            @if(task.archived())
                <p>Архив</p>
            @endif

            <div style="display: flex; gap: 20px; align-items: center; flex-wrap: wrap;">
                <div>
                    <span>Статус:</span>

                    @if(task.canUpdateStatus())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/status"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <select
                                name="status"
                                data-inline-control
                                data-original-value="${task.status().name()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                <option value="${task.status().name()}" selected>
                                    ${task.status().name()}
                                </option>

                                @for(TaskStatus status : TaskStatus.values())
                                    @if(status != task.status())
                                        <option value="${status.name()}">
                                            ${status.name()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            <span data-inline-actions style="display: none;">
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button type="button" onclick="cancelInlineUpdate(this)">
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @else
                        <span>${task.status().name()}</span>
                    @endif
                </div>

                <div>
                    <span>Автор:</span>

                    @if(!task.canUpdateTask() || members.size() == 1)
                        <span>${task.authorName()}</span>
                    @else
                        <form
                            method="post"
                            action="/tasks/${task.id()}/author"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <select
                                name="authorId"
                                data-inline-control
                                data-original-value="${task.authorId().toString()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                <option value="${task.authorId().toString()}" selected>
                                    ${task.authorName()}
                                </option>

                                @for(User member : members)
                                    @if(!member.getId().equals(task.authorId()))
                                        <option value="${member.getId().toString()}">
                                            ${member.getName()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            @if(task.showAuthorChangeWarning())
                                <p style="color: #9a6700; margin: 4px 0 0 0;">
                                    После смены автора предыдущий автор потеряет право редактировать задачу.
                                </p>
                            @endif

                            <span data-inline-actions style="display: none;">
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button type="button" onclick="cancelInlineUpdate(this)">
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <div>
                    <span>Исполнитель:</span>

                    @if(!task.canUpdateTask() || members.size() == 1)
                        <span>${task.assigneeName()}</span>
                    @else
                        <form
                            method="post"
                            action="/tasks/${task.id()}/assignee"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <select
                                name="assigneeId"
                                data-inline-control
                                data-original-value="${task.assigneeId().toString()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                <option value="${task.assigneeId().toString()}" selected>
                                    ${task.assigneeName()}
                                </option>

                                @for(User member : members)
                                    @if(!member.getId().equals(task.assigneeId()))
                                        <option value="${member.getId().toString()}">
                                            ${member.getName()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            <span data-inline-actions style="display: none;">
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button type="button" onclick="cancelInlineUpdate(this)">
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <div>
                    <span>Дедлайн:</span>

                    @if(task.deadlineOverdue())
                        <span style="color: red;">
                            ${task.deadlineText()}
                        </span>
                    @else
                        <span>
                            ${task.deadlineText()}
                        </span>
                    @endif

                    @if(task.canUpdateTask())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/deadline"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            @if(task.deadlineAt() == null)
                                <input
                                    type="date"
                                    name="deadlineDate"
                                    data-inline-control
                                    data-original-value=""
                                    onchange="showInlineSavePanel(this)"
                                >
                            @else
                                <input
                                    type="date"
                                    name="deadlineDate"
                                    value="${task.deadlineAt().toString().substring(0, 10)}"
                                    data-inline-control
                                    data-original-value="${task.deadlineAt().toString().substring(0, 10)}"
                                    onchange="showInlineSavePanel(this)"
                                >
                            @endif

                            <span data-inline-actions style="display: none;">
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button type="button" onclick="cancelInlineUpdate(this)">
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @endif
                </div>

                <div>
                    <span>Категория:</span>

                    @if(task.canUpdateTask())
                        <form
                            method="post"
                            action="/tasks/${task.id()}/category"
                            style="display: inline;"
                            data-inline-update-form="true"
                        >
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <select
                                name="category"
                                data-inline-control
                                data-original-value="${task.category().name()}"
                                onchange="showInlineSavePanel(this)"
                            >
                                <option value="${task.category().name()}" selected>
                                    ${task.category().name()}
                                </option>

                                @for(TaskCategory category : categories)
                                    @if(category != task.category())
                                        <option value="${category.name()}">
                                            ${category.name()}
                                        </option>
                                    @endif
                                @endfor
                            </select>

                            <span data-inline-actions style="display: none;">
                                <button type="submit">
                                    Сохранить
                                </button>

                                <button type="button" onclick="cancelInlineUpdate(this)">
                                    Отмена
                                </button>
                            </span>
                        </form>
                    @else
                        <span>${task.category().name()}</span>
                    @endif
                </div>

                <div>
                    <span>Команда:</span>

                    <a href="/teams/${team.getId()}">
                        ${team.getName()}
                    </a>
                </div>

                @if(task.archived())
                    @if(task.canRestore())
                        <form method="post" action="/tasks/${task.id()}/restore" style="display: inline;">
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <button type="submit">
                                Вернуть из архива
                            </button>
                        </form>
                    @endif
                @else
                    @if(task.canArchive())
                        <form method="post" action="/tasks/${task.id()}/archive" style="display: inline;">
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                            <input type="hidden" name="returnTo" value="task">

                            <button type="submit">
                                Перенести в архив
                            </button>
                        </form>
                    @endif
                @endif
            </div>
        </section>

        <section>
            <h2>Описание</h2>

            <div style="border: 1px solid #d0d7de; padding: 16px; max-width: 720px;">
                @if(task.description() == null || task.description().isBlank())
                    <p>Описание пока не добавлено.</p>
                @else
                    <p>${task.description()}</p>
                @endif
            </div>

            @if(task.canUpdateTask())
                <a
                    href="#"
                    onclick="document.getElementById('description-edit-form').style.display = 'block'; this.style.display = 'none'; return false;"
                >
                    Редактировать описание
                </a>

                <form
                    id="description-edit-form"
                    method="post"
                    action="/tasks/${task.id()}/edit"
                    style="display: none; margin-top: 12px;"
                >
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                    <input type="hidden" name="title" value="${task.title()}">
                    <input type="hidden" name="category" value="${task.category().name()}">
                    <input type="hidden" name="assigneeId" value="${task.assigneeId().toString()}">

                    @if(task.deadlineAt() != null)
                        <input
                            type="hidden"
                            name="deadlineDate"
                            value="${task.deadlineAt().toString().substring(0, 10)}"
                        >
                    @endif

                    <div>
                        <label>Описание</label>

                        <textarea name="description" required>@if(task.description() != null)${task.description()}@endif</textarea>
                    </div>

                    <button type="submit">
                        Сохранить описание
                    </button>

                    <button
                        type="button"
                        onclick="document.getElementById('description-edit-form').style.display = 'none';"
                    >
                        Отмена
                    </button>
                </form>
            @endif
        </section>

        <section>
            <h2>Комментарии</h2>

            @if(comments.isEmpty())
                <p>Комментариев пока нет.</p>
            @else
                <ul>
                    @for(CommentView comment : comments)
                        <li>
                            <p>${comment.message()}</p>

                            <small>
                                ${comment.userName()} · ${comment.createdAtText()}

                                @if(comment.edited())
                                    <span style="color: gray; font-style: italic;">
                                        Отредактировано
                                    </span>
                                @endif
                            </small>

                            @if(comment.canEdit())
                                <div>
                                    <a
                                        href="#"
                                        onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'block'; return false;"
                                    >
                                        Редактировать
                                    </a>

                                    <form
                                        method="post"
                                        action="/tasks/${task.id()}/comments/${comment.id()}/delete"
                                        style="display: inline;"
                                    >
                                        <input
                                            type="hidden"
                                            name="${_csrf.getParameterName()}"
                                            value="${_csrf.getToken()}"
                                        >

                                        <button type="submit">
                                            Удалить
                                        </button>
                                    </form>
                                </div>

                                <form
                                    id="comment-edit-form-${comment.id()}"
                                    method="post"
                                    action="/tasks/${task.id()}/comments/${comment.id()}/edit"
                                    style="display: none; margin-top: 8px;"
                                >
                                    <input
                                        type="hidden"
                                        name="${_csrf.getParameterName()}"
                                        value="${_csrf.getToken()}"
                                    >

                                    <textarea name="message" required>${comment.message()}</textarea>

                                    <button type="submit">
                                        Сохранить
                                    </button>

                                    <button
                                        type="button"
                                        onclick="document.getElementById('comment-edit-form-${comment.id()}').style.display = 'none';"
                                    >
                                        Отмена
                                    </button>
                                </form>
                            @endif
                        </li>
                    @endfor
                </ul>
            @endif

            <form method="post" action="/tasks/${task.id()}/comments">
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">

                <div>
                    <label>Комментарий</label>
                    <textarea name="message" required></textarea>
                </div>

                <button type="submit">
                    Добавить комментарий
                </button>
            </form>
        </section>
    `
)