@import dev.gushchin.taskmanager.model.NotificationSort
@import dev.gushchin.taskmanager.view.NotificationsPageView
@import dev.gushchin.taskmanager.view.NotificationFeedItem
@import dev.gushchin.taskmanager.view.NotificationMessagePart
@import dev.gushchin.taskmanager.view.UserAvatarView
@import org.springframework.security.web.csrf.CsrfToken

@param NotificationsPageView page
@param CsrfToken _csrf
@param UserAvatarView currentUser
@param String errorMessage = null

@template.layout.page(
    title = "Уведомления",
    _csrf = _csrf,
    currentUser = currentUser,
    errorMessage = errorMessage,
    activeSection = "notifications",
    headerContext = @`
        <a class="app-team-link app-static-context" href="/notifications">Уведомления</a>
    `,
    content = @`
        <section class="workspace-page notifications-page">
            <nav class="local-tabs notifications-tabs" aria-label="Разделы уведомлений">
                <a class="${!page.unreadOnly() ? "local-tab-active" : ""}" href="/notifications" aria-current="${!page.unreadOnly() ? "page" : null}">
                    Все
                </a>
                <a class="${page.unreadOnly() ? "local-tab-active" : ""}" href="/notifications?unread=true" aria-current="${page.unreadOnly() ? "page" : null}">
                    Непрочитанные
                </a>
            </nav>

            @if(page.page().items().isEmpty())
                <div class="notifications-empty-toolbar" aria-hidden="true"></div>
                <div class="empty-state notification-empty-state">
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
                        <path d="M17.5 8.5a5.5 5.5 0 0 0-11 0c0 6.5-3 6.5-3 8.5h17c0-2-3-2-3-8.5Z"></path>
                        <path d="M10 20.5h4"></path>
                    </svg>

                    <h2>${page.unreadOnly() ? "Непрочитанных уведомлений нет" : "Уведомлений нет"}</h2>
                </div>
            @else
                <form id="notifications-read-form" class="task-toolbar notifications-toolbar" method="post" action="/notifications/read" data-notification-read-form data-submit-loading>
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    <input type="hidden" name="sort" value="${page.sort().name()}">
                    @if(page.unreadOnly())
                        <input type="hidden" name="unread" value="true">
                    @endif
                    <span class="task-count">Уведомления (${page.unreadOnly() ? page.counts().unread() : page.counts().all()})</span>
                    <div class="toolbar-popover task-sort-popover" data-toolbar-select>
                        <button class="primer-select-trigger task-toolbar-trigger" type="button" aria-haspopup="menu" aria-expanded="false">
                            <span class="primer-select-value">Сортировка</span>
                            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>
                        </button>
                        <div class="primer-select-panel task-sort-panel" hidden><div class="primer-select-options" role="menu">
                            <a class="primer-select-option" href="/notifications${page.unreadOnly() ? "?unread=true" : ""}" role="menuitem" aria-current="${page.sort() == NotificationSort.NEWEST ? "true" : null}">Сначала новые</a>
                            <a class="primer-select-option" href="/notifications?sort=OLDEST${page.unreadOnly() ? "&unread=true" : ""}" role="menuitem" aria-current="${page.sort() == NotificationSort.OLDEST ? "true" : null}">Сначала старые</a>
                        </div></div>
                    </div>
                </form>

                <div class="notification-list">
                    <label class="notification-list-header"><input type="checkbox" data-notification-select-all aria-label="Выбрать все уведомления"><span>Выделить все</span></label>
                    @for(NotificationFeedItem item : page.page().items())
                        <div class="notification-row ${item.notification().isRead() ? "notification-row-read" : "notification-row-unread"}">
                            <label class="notification-checkbox-control">
                                <input form="notifications-read-form" type="checkbox" name="notificationIds" value="${item.notification().getId()}" data-notification-select aria-label="Выбрать уведомление: ${item.message()}">
                            </label>
                            <span class="notification-unread-dot" aria-hidden="true"></span>
                            <span class="notification-message">
                                @if(item.destinationUrl() != null)
                                    <a class="notification-message-link" href="${item.destinationUrl()}" data-notification-open="${item.notification().getId()}">@for(NotificationMessagePart part : item.messageParts())@if(part.emphasized())<strong>${part.text()}</strong>@else${part.text()}@endif@endfor</a>
                                @else
                                    @for(NotificationMessagePart part : item.messageParts())@if(part.emphasized())<strong>${part.text()}</strong>@else${part.text()}@endif@endfor
                                @endif
                                @if(!item.notification().isRead())
                                    <span class="visually-hidden">Непрочитанное уведомление</span>
                                @endif
                            </span>
                            @if(item.invitationAction() != null && item.invitationAction().active())
                                <a class="button button-primary notification-invitation-action" href="${page.invitationUrl(item.invitationAction().token())}" data-notification-open="${item.notification().getId()}">Открыть приглашение</a>
                            @elseif(item.invitationAction() != null && item.invitationAction().expired())
                                <span class="invitation-status invitation-status-expired">Ссылка истекла</span>
                            @endif
                            <time datetime="${item.notification().getCreatedAt().toString()}" title="${item.notification().getCreatedAt().toString()}">${item.relativeTime()}</time>
                        </div>
                    @endfor
                </div>

                @if(page.nextPageUrl() != null)
                    <div class="notification-next-page" data-notification-next-page role="status" aria-live="polite">
                        <a class="button button-secondary" href="${page.nextPageUrl()}">Показать еще</a>
                    </div>
                @endif

                <div class="notification-selection-bar" data-notification-selection-bar role="status" aria-live="polite" hidden>
                    <span class="notification-selection-summary">Выбрано: <span data-notification-selection-count>0</span></span>
                    <div class="notification-selection-actions">
                        <button class="button button-secondary notification-selection-clear" type="button" data-notification-selection-clear>Сбросить</button>
                        <button class="button button-primary notification-read-button" form="notifications-read-form" type="submit" data-notification-read-submit data-loading-text="Обработка..." disabled>Прочитать</button>
                    </div>
                </div>
            @endif
        </section>

        @if(page.invitationDecision() != null)
            @template.invitations.show(invitation = page.invitationDecision(), _csrf = _csrf, returnTo = "notifications")
        @endif

        <script>
            (function () {
                const form = document.querySelector('[data-notification-read-form]');
                const selectAll = document.querySelector('[data-notification-select-all]');

                if (form === null || selectAll === null) {
                    return;
                }

                const submit = document.querySelector('[data-notification-read-submit]');
                const selectionBar = document.querySelector('[data-notification-selection-bar]');
                const selectionCount = selectionBar.querySelector('[data-notification-selection-count]');
                const clearSelection = selectionBar.querySelector('[data-notification-selection-clear]');
                const notificationList = document.querySelector('.notification-list');
                const checkboxes = [];

                function getCheckboxes() {
                    return checkboxes;
                }

                function updateSelection() {
                    const checkboxes = getCheckboxes();
                    const selectedCount = checkboxes.filter(function (checkbox) { return checkbox.checked; }).length;
                    submit.disabled = selectedCount === 0;
                    selectionBar.hidden = selectedCount === 0;
                    selectionCount.textContent = selectedCount;
                    selectAll.checked = selectedCount > 0 && selectedCount === checkboxes.length;
                    selectAll.indeterminate = selectedCount > 0 && selectedCount < checkboxes.length;
                }

                selectAll.addEventListener('change', function () {
                    getCheckboxes().forEach(function (checkbox) { checkbox.checked = selectAll.checked; });
                    updateSelection();
                });

                clearSelection.addEventListener('click', function () {
                    getCheckboxes().forEach(function (checkbox) { checkbox.checked = false; });
                    updateSelection();
                    selectAll.focus();
                });

                function bindCheckboxes(scope) {
                    scope.querySelectorAll('[data-notification-select]').forEach(function (checkbox) {
                        checkbox.addEventListener('change', updateSelection);
                        checkboxes.push(checkbox);
                    });
                    if (scope.matches !== undefined && scope.matches('[data-notification-select]')) {
                        scope.addEventListener('change', updateSelection);
                        checkboxes.push(scope);
                    }
                }

                bindCheckboxes(document);
                updateSelection();

                function notificationReadBody(link) {
                    const csrf = form.querySelector('input[type="hidden"]');
                    const body = new FormData();
                    body.append(csrf.name, csrf.value);
                    body.append('notificationId', link.dataset.notificationOpen);
                    return body;
                }

                function markAsReadInBackground(link) {
                    const body = notificationReadBody(link);
                    if (navigator.sendBeacon !== undefined
                            && navigator.sendBeacon('/notifications/read-on-open', body)) {
                        return;
                    }

                    fetch('/notifications/read-on-open', {
                        method: 'POST',
                        body: body,
                        credentials: 'same-origin',
                        keepalive: true
                    }).catch(function () {});
                }

                document.addEventListener('click', function (event) {
                    const link = event.target.closest('[data-notification-open]');
                    if (link === null || link.closest('.notification-row-unread') === null) {
                        return;
                    }

                    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.target === '_blank') {
                        markAsReadInBackground(link);
                        return;
                    }

                    event.preventDefault();
                    const destination = link.href;
                    let navigated = false;
                    function navigate() {
                        if (!navigated) {
                            navigated = true;
                            window.location.assign(destination);
                        }
                    }

                    fetch('/notifications/read-on-open', {
                        method: 'POST',
                        body: notificationReadBody(link),
                        credentials: 'same-origin',
                        keepalive: true
                    }).finally(navigate);
                    window.setTimeout(navigate, 800);
                });

                let loadingNextPage = false;
                let nextPageObserver = null;

                function observeNextPage() {
                    const nextPage = document.querySelector('[data-notification-next-page]');
                    if (nextPageObserver !== null) {
                        nextPageObserver.disconnect();
                        nextPageObserver = null;
                    }
                    if (nextPage === null || !('IntersectionObserver' in window)) {
                        return;
                    }
                    nextPageObserver = new IntersectionObserver(function (entries) {
                        if (entries[0].isIntersecting) {
                            loadNextPage(nextPage.querySelector('a'));
                        }
                    }, { rootMargin: '240px 0px' });
                    nextPageObserver.observe(nextPage);
                }

                async function loadNextPage(link) {
                    if (link === null || loadingNextPage) {
                        return;
                    }

                    loadingNextPage = true;
                    const selectAllWasChecked = selectAll.checked;
                    const originalText = link.textContent;
                    link.textContent = 'Загрузка...';
                    link.setAttribute('aria-disabled', 'true');

                    try {
                        const response = await fetch(link.href, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
                        if (!response.ok) {
                            throw new Error('Notification page request failed');
                        }

                        const documentFragment = new DOMParser().parseFromString(await response.text(), 'text/html');
                        const loadedList = documentFragment.querySelector('.notification-list');
                        if (loadedList === null || notificationList === null) {
                            throw new Error('Notification page response is incomplete');
                        }

                        loadedList.querySelectorAll('.notification-row').forEach(function (row) {
                            notificationList.append(row);
                            bindCheckboxes(row);
                            if (selectAllWasChecked) {
                                row.querySelector('[data-notification-select]').checked = true;
                            }
                        });

                        const currentNextPage = document.querySelector('[data-notification-next-page]');
                        const loadedNextPage = documentFragment.querySelector('[data-notification-next-page]');
                        if (loadedNextPage === null) {
                            currentNextPage.remove();
                        } else {
                            currentNextPage.replaceWith(loadedNextPage);
                        }
                        updateSelection();
                        observeNextPage();
                    } catch (error) {
                        link.textContent = 'Не удалось загрузить уведомления. Попробуйте еще раз';
                        link.classList.add('notification-next-page-error');
                    } finally {
                        loadingNextPage = false;
                        link.removeAttribute('aria-disabled');
                        if (link.isConnected && link.textContent === 'Загрузка...') {
                            link.textContent = originalText;
                        }
                    }
                }

                document.addEventListener('click', function (event) {
                    const link = event.target.closest('[data-notification-next-page] a');
                    if (link === null) {
                        return;
                    }
                    event.preventDefault();
                    loadNextPage(link);
                });
                observeNextPage();
            }());
        </script>
    `
)
