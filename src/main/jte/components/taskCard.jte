@import dev.gushchin.taskmanager.view.TaskView

@param TaskView task
@param Long teamId
@param String teamName

<li class="task-card task-card-status-${task.statusCssClass()}">
    <div class="task-card-header">
        <span class="task-card-status">${task.statusText()}</span>

        <button
            class="task-card-menu"
            type="button"
            aria-label="Действия с задачей «${task.title()}»"
        >
            <svg
                aria-hidden="true"
                viewBox="0 0 16 16"
                width="16"
                height="16"
                fill="currentColor"
            >
                <path d="M2 7a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Zm6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z"></path>
            </svg>
        </button>
    </div>

    <div class="task-card-body">
        <h3>
            <a href="/tasks/${task.id()}">${task.title()}</a>
        </h3>

        <div class="task-card-labels">
            <span class="task-card-tag">${task.tagName()}</span>
            <span class="task-card-deadline ${task.deadlineToday() || task.deadlineOverdue() ? "task-card-deadline-urgent" : ""}">
                ${task.deadlineText()}
            </span>
        </div>

        <dl class="task-card-people">
            <div>
                <dt>Автор</dt>
                <dd class="${task.authorRemovedFromTeam() ? "task-card-former-member" : ""}">
                    ${task.authorName()}
                </dd>
            </div>
            <div>
                <dt>Исполнитель</dt>
                <dd class="${task.assigneeRemovedFromTeam() ? "task-card-former-member" : ""}">
                    ${task.assigneeName()}
                </dd>
            </div>
        </dl>

        <footer class="task-card-footer">
            <time datetime="${task.updatedAt() == null ? "" : task.updatedAt().toString()}">${task.updatedAtText()}</time>

            @if(teamId != null && teamName != null)
                <a href="/teams/${teamId}">${teamName}</a>
            @endif
        </footer>
    </div>
</li>
