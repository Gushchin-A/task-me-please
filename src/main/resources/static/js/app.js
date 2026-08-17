document.addEventListener('DOMContentLoaded', function () {
    setupProfileMenu();
    setupFlashMessages();
    setupTooltips();
    setupSubmitLoading();
    setupToolbarPopovers();
    setupToolbarSelects();
    setupFilterSelects();
    setupTeamSettings();
    setupTaskCards();
});

function setupTaskCards() {
    document.querySelectorAll('[data-task-link-copy]').forEach(function (button) {
        button.addEventListener('click', async function () {
            const taskUrl = new URL(button.dataset.taskUrl, window.location.origin).href;

            try {
                await navigator.clipboard.writeText(taskUrl);
            } catch (error) {
                const input = document.createElement('textarea');

                input.value = taskUrl;
                input.style.position = 'fixed';
                input.style.opacity = '0';
                document.body.appendChild(input);
                input.select();
                document.execCommand('copy');
                input.remove();
            }

            button.closest('.task-card-actions').open = false;
            showFlashMessage('Ссылка на задачу скопирована.');
        });
    });

    document.querySelectorAll('[data-task-edit-dialog]').forEach(function (dialog) {
        const card = dialog.closest('.task-card');
        const openButton = card.querySelector('[data-task-edit-open]');
        const form = dialog.querySelector('[data-task-edit-form]');
        const submit = dialog.querySelector('[data-task-edit-submit]');
        const titleInput = dialog.querySelector('input[name="title"]');
        const titleLength = dialog.querySelector('[data-task-title-length]');
        const selects = dialog.querySelectorAll('[data-task-edit-select]');
        const closeButtons = dialog.querySelectorAll('[data-task-edit-close], [data-task-edit-cancel]');
        const originalValues = new URLSearchParams(new FormData(form)).toString();

        function closeSelect(select) {
            const trigger = select.querySelector(':scope > .primer-select-trigger');

            trigger.nextElementSibling.hidden = true;
            trigger.setAttribute('aria-expanded', 'false');
        }

        function closeSelects(exceptSelect) {
            selects.forEach(function (select) {
                if (select !== exceptSelect) {
                    closeSelect(select);
                }
            });
        }

        function resetTaskForm() {
            form.reset();
            titleLength.textContent = titleInput.value.length;

            selects.forEach(function (select) {
                const hiddenInput = select.parentElement.querySelector('input[type="hidden"]');
                const selectedOption = Array.from(select.querySelectorAll('.primer-select-option'))
                        .find(function (option) {
                            return option.dataset.value === hiddenInput.value;
                        });
                const value = select.querySelector('.primer-select-value');
                const selectedAvatar = select.querySelector('[data-selected-avatar]');

                select.querySelectorAll('.primer-select-option').forEach(function (option) {
                    option.setAttribute('aria-selected', String(option === selectedOption));
                });

                if (selectedOption) {
                    value.textContent = selectedOption.dataset.label;
                    value.title = selectedOption.dataset.fullLabel || selectedOption.dataset.label;

                    if (selectedAvatar && selectedOption.dataset.avatar) {
                        selectedAvatar.textContent = selectedOption.dataset.avatar;
                    }
                }
            });
        }

        function updateSubmitState() {
            submit.disabled = new URLSearchParams(new FormData(form)).toString() === originalValues;
        }

        function closeDialog() {
            closeSelects();
            dialog.close();
            resetTaskForm();
            updateSubmitState();
            openButton.focus();
        }

        selects.forEach(function (select) {
            const trigger = select.querySelector(':scope > .primer-select-trigger');
            const panel = trigger.nextElementSibling;

            trigger.addEventListener('click', function (event) {
                event.stopPropagation();

                const shouldOpen = panel.hidden;

                closeSelects(select);
                panel.hidden = !shouldOpen;
                trigger.setAttribute('aria-expanded', String(shouldOpen));

                if (shouldOpen) {
                    const selectedOption = panel.querySelector('[aria-selected="true"]');

                    selectedOption?.focus();
                }
            });

            select.querySelectorAll('.primer-select-option').forEach(function (option) {
                option.addEventListener('click', function () {
                    const hiddenInput = select.parentElement.querySelector('input[type="hidden"]');
                    const value = select.querySelector('.primer-select-value');
                    const selectedAvatar = select.querySelector('[data-selected-avatar]');

                    hiddenInput.value = option.dataset.value;
                    value.textContent = option.dataset.label;
                    value.title = option.dataset.fullLabel || option.dataset.label;

                    if (selectedAvatar && option.dataset.avatar) {
                        selectedAvatar.textContent = option.dataset.avatar;
                    }

                    select.querySelectorAll('.primer-select-option').forEach(function (otherOption) {
                        otherOption.setAttribute('aria-selected', String(otherOption === option));
                    });

                    closeSelect(select);
                    hiddenInput.dispatchEvent(new Event('change', {bubbles: true}));
                    trigger.focus();
                });
            });
        });

        openButton.addEventListener('click', function () {
            card.querySelector('.task-card-actions').open = false;
            resetTaskForm();
            updateSubmitState();
            dialog.showModal();
            titleInput.focus();
        });

        form.addEventListener('input', updateSubmitState);
        form.addEventListener('change', updateSubmitState);
        titleInput.addEventListener('input', function () {
            titleLength.textContent = titleInput.value.length;
        });

        closeButtons.forEach(function (button) {
            button.addEventListener('click', closeDialog);
        });

        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) {
                closeDialog();
            } else if (!event.target.closest('[data-task-edit-select]')) {
                closeSelects();
            }
        });

        dialog.addEventListener('cancel', function (event) {
            event.preventDefault();

            const openSelect = Array.from(selects).find(function (select) {
                return select.querySelector(':scope > .primer-select-trigger')
                        .getAttribute('aria-expanded') === 'true';
            });

            if (openSelect) {
                closeSelect(openSelect);
                openSelect.querySelector(':scope > .primer-select-trigger').focus();
            } else {
                closeDialog();
            }
        });
    });
}

function setupTeamSettings() {
    const renameForm = document.querySelector('[data-settings-rename-form]');

    if (renameForm !== null) {
        const input = renameForm.querySelector('[data-settings-name]');
        const submit = renameForm.querySelector('[data-settings-rename-submit]');

        setupChangedValueSubmitState(input, submit);
    }

    const dialog = document.querySelector('[data-tag-rename-dialog]');

    if (dialog === null) {
        return;
    }

    const form = dialog.querySelector('[data-tag-rename-form]');
    const input = dialog.querySelector('[data-tag-rename-input]');
    const submit = dialog.querySelector('[data-tag-rename-submit]');

    document.querySelectorAll('[data-tag-rename-open]').forEach(function (button) {
        button.addEventListener('click', function () {
            form.action = window.location.pathname + '/tags/' + button.dataset.tagId + '/rename';
            input.value = button.dataset.tagName;
            input.dataset.originalValue = button.dataset.tagName;
            submit.disabled = true;
            dialog.showModal();
            input.focus();
            input.select();
        });
    });

    setupChangedValueSubmitState(input, submit);

    dialog.querySelector('[data-tag-rename-close]').addEventListener('click', function () {
        dialog.close();
    });

    dialog.addEventListener('click', function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });
}

function setupChangedValueSubmitState(input, submit) {
    input.addEventListener('input', function () {
        submit.disabled = input.value === input.dataset.originalValue;
    });
}

function setupFilterSelects() {
    const selects = document.querySelectorAll('[data-filter-select]');

    function closeFilterSelect(select) {
        const trigger = select.querySelector(':scope > .primer-select-trigger');
        const panel = trigger.nextElementSibling;

        panel.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
    }

    selects.forEach(function (select) {
        const trigger = select.querySelector(':scope > .primer-select-trigger');
        const panel = trigger.nextElementSibling;

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();

            const shouldOpen = panel.hidden;

            selects.forEach(function (otherSelect) {
                if (otherSelect !== select) {
                    closeFilterSelect(otherSelect);
                }
            });

            panel.hidden = !shouldOpen;
            trigger.setAttribute('aria-expanded', String(shouldOpen));
        });
    });

    document.addEventListener('click', function (event) {
        selects.forEach(function (select) {
            if (!select.contains(event.target)) {
                closeFilterSelect(select);
            }
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }

        const openSelect = Array.from(selects).find(function (select) {
            return select.querySelector(':scope > .primer-select-trigger')
                    .getAttribute('aria-expanded') === 'true';
        });

        if (openSelect !== undefined) {
            const trigger = openSelect.querySelector(':scope > .primer-select-trigger');

            event.stopImmediatePropagation();
            closeFilterSelect(openSelect);
            trigger.focus();
        }
    });
}

function setupToolbarSelects() {
    const selects = document.querySelectorAll('[data-toolbar-select]');

    function closeToolbarSelect(select) {
        const trigger = select.querySelector('.task-toolbar-trigger');
        const panel = trigger.nextElementSibling;

        panel.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        select.querySelectorAll('[data-filter-select]').forEach(function (filterSelect) {
            const filterTrigger = filterSelect.querySelector(':scope > .primer-select-trigger');

            filterTrigger.nextElementSibling.hidden = true;
            filterTrigger.setAttribute('aria-expanded', 'false');
        });
    }

    selects.forEach(function (select) {
        const trigger = select.querySelector('.task-toolbar-trigger');
        const panel = trigger.nextElementSibling;
        const closeButton = panel.querySelector('[data-toolbar-select-close]');

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();

            const shouldOpen = panel.hidden;

            selects.forEach(function (otherSelect) {
                if (otherSelect !== select) {
                    closeToolbarSelect(otherSelect);
                }
            });

            panel.hidden = !shouldOpen;
            trigger.setAttribute('aria-expanded', String(shouldOpen));
        });

        closeButton.addEventListener('click', function (event) {
            event.stopPropagation();
            closeToolbarSelect(select);
            trigger.focus();
        });
    });

    document.addEventListener('click', function (event) {
        selects.forEach(function (select) {
            if (!select.contains(event.target)) {
                closeToolbarSelect(select);
            }
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }

        selects.forEach(function (select) {
            const trigger = select.querySelector('.task-toolbar-trigger');

            if (trigger.getAttribute('aria-expanded') === 'true') {
                closeToolbarSelect(select);
                trigger.focus();
            }
        });
    });
}

function setupToolbarPopovers() {
    const popovers = document.querySelectorAll('.toolbar-popover, .comment-menu, .task-card-actions, .team-switcher');

    popovers.forEach(function (popover) {
        const closeButton = popover.querySelector('[data-team-switcher-close]');

        if (closeButton !== null) {
            closeButton.addEventListener('click', function () {
                popover.open = false;
                popover.querySelector('summary').focus();
            });
        }

        popover.addEventListener('toggle', function () {
            if (!popover.open) {
                return;
            }

            popovers.forEach(function (otherPopover) {
                if (otherPopover !== popover) {
                    otherPopover.open = false;
                }
            });
        });
    });

    document.addEventListener('click', function (event) {
        popovers.forEach(function (popover) {
            if (!popover.contains(event.target)) {
                popover.open = false;
            }
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }

        popovers.forEach(function (popover) {
            if (popover.open) {
                popover.open = false;
                popover.querySelector('summary').focus();
            }
        });
    });
}

function setupProfileMenu() {
    const profile = document.querySelector('[data-profile]');

    if (profile === null) {
        return;
    }

    const trigger = profile.querySelector('[data-profile-trigger]');
    const menu = profile.querySelector('[data-profile-menu]');
    const firstAction = menu.querySelector('[data-profile-focus]');

    function closeProfileMenu() {
        menu.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
    }

    trigger.addEventListener('click', function () {
        const shouldOpen = menu.hidden;

        if (shouldOpen) {
            menu.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            if (firstAction !== null) {
                firstAction.focus();
            }
            return;
        }

        closeProfileMenu();
    });

    document.addEventListener('click', function (event) {
        if (!profile.contains(event.target)) {
            closeProfileMenu();
        }
    });

    document.addEventListener('focusin', function (event) {
        if (!profile.contains(event.target)) {
            closeProfileMenu();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !menu.hidden) {
            closeProfileMenu();
            trigger.focus();
        }
    });
}

function setupFlashMessages() {
    document.querySelectorAll('[data-flash-message]').forEach(function (message) {
        const closeButton = message.querySelector('[data-flash-close]');
        const dismissDelay = Number(message.dataset.autoDismissMs);

        if (closeButton !== null) {
            closeButton.addEventListener('click', function () {
                message.remove();
            });
        }

        if (dismissDelay > 0) {
            window.setTimeout(function () {
                message.remove();
            }, dismissDelay);
        }
    });
}

function setupTooltips() {
    const tooltip = document.createElement('div');
    let showTimer = null;

    tooltip.className = 'ui-tooltip';
    tooltip.setAttribute('role', 'tooltip');
    tooltip.hidden = true;
    document.body.appendChild(tooltip);

    document.querySelectorAll('[title]').forEach(function (element) {
        if (!element.dataset.tooltip) {
            element.dataset.tooltip = element.getAttribute('title');
        }
        element.removeAttribute('title');
    });

    document.querySelectorAll('[data-tooltip]').forEach(function (element) {
        function hideTooltip() {
            window.clearTimeout(showTimer);
            tooltip.hidden = true;
        }

        function scheduleTooltip() {
            window.clearTimeout(showTimer);
            showTimer = window.setTimeout(function () {
                tooltip.textContent = element.dataset.tooltip;
                tooltip.hidden = false;

                const elementRect = element.getBoundingClientRect();
                const tooltipRect = tooltip.getBoundingClientRect();
                const left = Math.min(
                        window.innerWidth - tooltipRect.width - 12,
                        Math.max(12, elementRect.left + elementRect.width / 2 - tooltipRect.width / 2));
                const top = Math.min(
                        window.innerHeight - tooltipRect.height - 12,
                        elementRect.bottom + 8);

                tooltip.style.left = left + 'px';
                tooltip.style.top = top + 'px';
            }, 200);
        }

        element.addEventListener('mouseenter', scheduleTooltip);
        element.addEventListener('mouseleave', hideTooltip);
        element.addEventListener('focus', scheduleTooltip);
        element.addEventListener('blur', hideTooltip);
    });
}

function setupSubmitLoading() {
    document.querySelectorAll('[data-submit-loading]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            const submitButton = event.submitter === null
                    ? form.querySelector('button[type="submit"]')
                    : event.submitter;

            if (submitButton === null || submitButton.disabled) {
                return;
            }

            const loadingText = submitButton.dataset.loadingText;
            const spinner = document.createElement('span');

            spinner.className = 'auth-spinner';
            spinner.setAttribute('aria-hidden', 'true');
            form.setAttribute('aria-busy', 'true');
            submitButton.disabled = true;
            submitButton.replaceChildren(spinner, document.createTextNode(loadingText));
        });
    });
}

function showFlashMessage(message) {
    let stack = document.querySelector('.flash-stack');

    if (stack === null) {
        stack = document.createElement('div');
        stack.className = 'flash-stack';
        stack.setAttribute('aria-live', 'polite');
        document.body.appendChild(stack);
    }

    const flashMessage = document.createElement('div');
    const messageText = document.createElement('span');
    const closeButton = document.createElement('button');

    flashMessage.className = 'flash-message flash-message-info';
    flashMessage.dataset.flashMessage = '';
    flashMessage.dataset.autoDismissMs = '10000';
    flashMessage.setAttribute('role', 'status');
    messageText.className = 'flash-message-text';
    messageText.textContent = message;
    closeButton.className = 'flash-close';
    closeButton.type = 'button';
    closeButton.dataset.flashClose = '';
    closeButton.setAttribute('aria-label', 'Закрыть сообщение');
    closeButton.textContent = '×';
    flashMessage.append(messageText, closeButton);
    stack.appendChild(flashMessage);

    closeButton.addEventListener('click', function () {
        flashMessage.remove();
    });
    window.setTimeout(function () {
        flashMessage.remove();
    }, 10000);
}
