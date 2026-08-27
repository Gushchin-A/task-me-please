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
    setupTaskDetail();
});

function setupTaskDetail() {
    const detail = document.querySelector('[data-task-detail]');

    if (detail === null) {
        return;
    }

    const titleRow = detail.querySelector('.task-title-row');
    const titleOpen = detail.querySelector('[data-task-title-edit-open]');
    const titleForm = detail.querySelector('[data-task-title-edit-form]');
    const titleCancel = detail.querySelector('[data-task-title-edit-cancel]');

    if (titleOpen !== null && titleForm !== null && titleCancel !== null) {
        const titleInput = titleForm.querySelector('input[name="title"]');
        const titleSubmit = titleForm.querySelector('[data-task-title-edit-submit]');
        const titleLength = titleForm.querySelector('[data-task-detail-title-length]');

        setupChangedValueSubmitState(titleInput, titleSubmit);

        titleInput.addEventListener('input', function () {
            titleLength.textContent = titleInput.value.length;
        });

        titleOpen.addEventListener('click', function () {
            titleRow.hidden = true;
            titleForm.hidden = false;
            titleSubmit.disabled = true;
            titleInput.focus();
            titleInput.select();
        });

        titleCancel.addEventListener('click', function () {
            titleForm.reset();
            titleSubmit.disabled = true;
            titleLength.textContent = titleInput.value.length;
            titleForm.hidden = true;
            titleRow.hidden = false;
            titleOpen.focus();
        });
    }

    const descriptionOpen = detail.querySelector('[data-description-edit-open]');
    const descriptionView = detail.querySelector('[data-description-view]');
    const descriptionForm = detail.querySelector('[data-description-edit-form]');
    const descriptionCancel = detail.querySelector('[data-description-edit-cancel]');

    if (descriptionOpen !== null && descriptionView !== null
            && descriptionForm !== null && descriptionCancel !== null) {
        const descriptionInput = descriptionForm.querySelector('textarea[name="description"]');
        const descriptionSubmit = descriptionForm.querySelector('[data-changed-value-submit]');

        function updateDescriptionSubmitState() {
            descriptionSubmit.disabled = descriptionInput.value === descriptionInput.dataset.originalValue;
        }

        descriptionOpen.addEventListener('click', function () {
            descriptionView.hidden = true;
            descriptionForm.hidden = false;
            descriptionOpen.closest('details')?.removeAttribute('open');
            updateDescriptionSubmitState();
            descriptionInput.focus();
        });

        descriptionCancel.addEventListener('click', function () {
            descriptionForm.reset();
            updateDescriptionSubmitState();
            descriptionForm.hidden = true;
            descriptionView.hidden = false;
            descriptionOpen.focus();
        });

        descriptionInput.addEventListener('input', updateDescriptionSubmitState);
    }

    detail.querySelectorAll('[data-comment-edit-form]').forEach(function (form) {
        const card = form.closest('.comment-card');
        const view = card.querySelector('[data-comment-view]');
        const open = card.querySelector('[data-comment-edit-open]');
        const cancel = form.querySelector('[data-comment-edit-cancel]');
        const input = form.querySelector('textarea[name="message"]');
        const submit = form.querySelector('[data-changed-value-submit]');

        function updateSubmitState() {
            submit.disabled = input.value === input.dataset.originalValue;
        }

        open.addEventListener('click', function () {
            view.hidden = true;
            form.hidden = false;
            open.closest('details')?.removeAttribute('open');
            updateSubmitState();
            input.focus();
        });

        cancel.addEventListener('click', function () {
            form.reset();
            updateSubmitState();
            form.hidden = true;
            view.hidden = false;
            open.focus();
        });

        input.addEventListener('input', updateSubmitState);
    });

    detail.querySelectorAll('.task-comment-editor').forEach(function (editor) {
        const input = editor.querySelector('textarea');
        const preview = editor.querySelector('[data-description-preview]');
        const tabs = editor.querySelectorAll('[data-editor-mode]');

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                const writeMode = tab.dataset.editorMode === 'write';

                tabs.forEach(function (item) {
                    const selected = item === tab;

                    item.classList.toggle('task-editor-tab-active', selected);
                    item.setAttribute('aria-selected', String(selected));
                    item.setAttribute('tabindex', selected ? '0' : '-1');
                });

                const editorHeight = input.offsetHeight + 16;

                input.hidden = !writeMode;
                preview.hidden = writeMode;
                editor.classList.toggle('task-description-editor-preview', !writeMode);

                if (!writeMode) {
                    preview.textContent = input.value;
                    preview.style.height = editorHeight + 'px';
                }
            });
        });

        editor.querySelectorAll('[data-format]').forEach(function (button) {
            button.addEventListener('click', function () {
                const start = input.selectionStart;
                const end = input.selectionEnd;
                const selectedText = input.value.slice(start, end) || 'текст';
                let formattedText = '> ' + selectedText;

                if (button.dataset.format === 'bold') {
                    formattedText = '**' + selectedText + '**';
                }

                if (button.dataset.format === 'italic') {
                    formattedText = '_' + selectedText + '_';
                }

                input.setRangeText(formattedText, start, end, 'end');
                input.dispatchEvent(new Event('input', {bubbles: true}));
                input.focus();
            });
        });
    });

    detail.querySelectorAll('.task-file-placeholder').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.preventDefault();
        });
    });

    const newCommentInput = detail.querySelector('.comment-create-form textarea[name="message"]');
    const newCommentSubmit = detail.querySelector('[data-new-comment-submit]');

    if (newCommentInput !== null && newCommentSubmit !== null) {
        newCommentInput.addEventListener('input', function () {
            newCommentSubmit.disabled = newCommentInput.value.trim() === '';
        });
    }

    detail.querySelectorAll('[data-task-anchor-copy]').forEach(function (button) {
        button.addEventListener('click', async function () {
            const taskUrl = new URL(window.location.href);

            taskUrl.search = '';
            taskUrl.hash = button.dataset.taskAnchorCopy;

            try {
                await navigator.clipboard.writeText(taskUrl.href);
            } catch (error) {
                const input = document.createElement('textarea');

                input.value = taskUrl.href;
                input.style.position = 'fixed';
                input.style.opacity = '0';
                document.body.appendChild(input);
                input.select();
                document.execCommand('copy');
                input.remove();
            }

            button.closest('.task-card-actions').open = false;
            showFlashMessage('Ссылка скопирована');
        });
    });

    const parameters = detail.querySelector('[data-task-parameters]');

    if (parameters === null) {
        return;
    }

    const editOpen = parameters.querySelector('[data-task-parameters-edit-open]');
    const forms = Array.from(parameters.querySelectorAll('[data-task-parameter-form]'));
    const values = Array.from(parameters.querySelectorAll('[data-task-parameter-value]'));
    const actions = parameters.querySelector('[data-task-parameters-actions]');
    const cancel = parameters.querySelector('[data-task-parameters-cancel]');
    const save = parameters.querySelector('[data-task-parameters-save]');
    const stateAction = parameters.querySelector('[data-task-parameters-state-action]');
    const parameterSelects = [];

    if (editOpen === null || actions === null || cancel === null || save === null) {
        return;
    }

    function closeParameterSelect(select) {
        select.panel.hidden = true;
        select.trigger.setAttribute('aria-expanded', 'false');
    }

    function closeParameterSelects(exceptSelect) {
        parameterSelects.forEach(function (select) {
            if (select !== exceptSelect) {
                closeParameterSelect(select);
            }
        });
    }

    function syncParameterSelect(select) {
        const selectedOption = Array.from(select.nativeSelect.options).find(function (option) {
            return option.value === select.nativeSelect.value;
        });

        if (selectedOption === undefined) {
            return;
        }

        select.value.textContent = selectedOption.textContent;
        select.options.forEach(function (option) {
            option.setAttribute('aria-selected', String(option.dataset.value === selectedOption.value));
        });

        if (select.avatar !== null) {
            select.avatar.textContent = selectedOption.textContent.trim().charAt(0).toUpperCase();
        }
    }

    forms.forEach(function (form) {
        const nativeSelect = form.querySelector('select');

        if (nativeSelect === null) {
            return;
        }

        const row = form.closest('.task-parameter-row');
        const heading = row.querySelector('.task-parameter-heading').textContent.trim();
        const select = document.createElement('div');
        const trigger = document.createElement('button');
        const value = document.createElement('span');
        const panel = document.createElement('div');
        const panelHeading = document.createElement('div');
        const options = document.createElement('div');
        const avatar = nativeSelect.hasAttribute('data-participant-select')
                ? document.createElement('span') : null;

        select.className = 'primer-select task-parameter-select';
        trigger.className = 'button primer-select-trigger';
        trigger.type = 'button';
        trigger.disabled = nativeSelect.disabled;
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');
        value.className = 'primer-select-value';
        panel.className = 'primer-select-panel';
        panel.hidden = true;
        panelHeading.className = 'primer-select-heading';
        panelHeading.textContent = 'Выберите ' + heading.toLowerCase();
        options.className = 'primer-select-options';
        options.setAttribute('role', 'listbox');

        if (avatar !== null) {
            avatar.className = 'participant-avatar';
            avatar.setAttribute('aria-hidden', 'true');
            trigger.appendChild(avatar);
        }

        trigger.appendChild(value);
        trigger.insertAdjacentHTML('beforeend', '<svg aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="m4.427 6.427 3.396 3.396a.25.25 0 0 0 .354 0l3.396-3.396A.25.25 0 0 0 11.396 6H4.604a.25.25 0 0 0-.177.427Z"></path></svg>');
        panel.appendChild(panelHeading);
        panel.appendChild(options);
        select.appendChild(trigger);
        select.appendChild(panel);
        nativeSelect.hidden = true;
        form.appendChild(select);

        const parameterSelect = {
            nativeSelect: nativeSelect,
            trigger: trigger,
            value: value,
            panel: panel,
            avatar: avatar,
            options: []
        };

        Array.from(nativeSelect.options).forEach(function (nativeOption) {
            const option = document.createElement('button');

            option.className = 'primer-select-option';
            option.type = 'button';
            option.dataset.value = nativeOption.value;
            option.setAttribute('role', 'option');
            option.innerHTML = '<span class="primer-select-check" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><path d="M13.78 4.22a.75.75 0 0 1 0 1.06l-7.25 7.25a.75.75 0 0 1-1.06 0L2.22 9.28a.75.75 0 1 1 1.06-1.06L6 10.94l6.72-6.72a.75.75 0 0 1 1.06 0Z"></path></svg></span>';

            if (avatar !== null) {
                const optionAvatar = document.createElement('span');

                optionAvatar.className = 'participant-avatar';
                optionAvatar.setAttribute('aria-hidden', 'true');
                optionAvatar.textContent = nativeOption.textContent.trim().charAt(0).toUpperCase();
                option.appendChild(optionAvatar);
            }

            const copy = document.createElement('span');

            copy.className = 'primer-select-option-copy';
            copy.textContent = nativeOption.textContent;
            option.appendChild(copy);
            options.appendChild(option);
            parameterSelect.options.push(option);

            option.addEventListener('click', function () {
                nativeSelect.value = option.dataset.value;
                syncParameterSelect(parameterSelect);
                closeParameterSelect(parameterSelect);
                nativeSelect.dispatchEvent(new Event('change', {bubbles: true}));
                trigger.focus();
            });
        });

        trigger.addEventListener('click', function (event) {
            event.stopPropagation();

            const shouldOpen = panel.hidden;

            closeParameterSelects(parameterSelect);
            panel.hidden = !shouldOpen;
            trigger.setAttribute('aria-expanded', String(shouldOpen));

            if (shouldOpen) {
                panel.querySelector('[aria-selected="true"]')?.focus();
            }
        });

        parameterSelects.push(parameterSelect);
        syncParameterSelect(parameterSelect);
    });

    function hasChanges() {
        return forms.some(function (form) {
            const control = form.querySelector('select, input[type="date"]');

            return !control.disabled && control.value !== control.dataset.originalValue;
        });
    }

    function updateSaveState() {
        save.disabled = !hasChanges();
    }

    function closeEditor() {
        forms.forEach(function (form) {
            form.reset();
            form.hidden = true;
        });
        parameterSelects.forEach(function (select) {
            closeParameterSelect(select);
            syncParameterSelect(select);
        });
        values.forEach(function (value) {
            value.hidden = false;
        });
        actions.hidden = true;
        if (stateAction !== null) {
            stateAction.hidden = false;
        }
        editOpen.hidden = false;
        updateSaveState();
        editOpen.focus();
    }

    editOpen.addEventListener('click', function () {
        values.forEach(function (value) {
            value.hidden = true;
        });
        forms.forEach(function (form) {
            form.hidden = false;
        });
        actions.hidden = false;
        if (stateAction !== null) {
            stateAction.hidden = true;
        }
        editOpen.hidden = true;
        updateSaveState();

        const firstEnabledControl = parameters.querySelector('.primer-select-trigger:not(:disabled), input[type="date"]:not(:disabled)');

        firstEnabledControl?.focus();
    });

    forms.forEach(function (form) {
        form.addEventListener('change', updateSaveState);
        form.addEventListener('input', updateSaveState);
    });

    cancel.addEventListener('click', closeEditor);

    document.addEventListener('click', function (event) {
        if (!event.target.closest('.task-parameter-select')) {
            closeParameterSelects();
        }
    });

    save.addEventListener('click', async function () {
        const changedForms = forms.filter(function (form) {
            const control = form.querySelector('select, input[type="date"]');

            return !control.disabled && control.value !== control.dataset.originalValue;
        });

        save.disabled = true;

        try {
            for (const form of changedForms) {
                const response = await fetch(form.action, {
                    method: 'POST',
                    body: new FormData(form),
                    credentials: 'same-origin'
                });

                if (!response.ok) {
                    throw new Error('Task parameters update failed');
                }
            }

            window.location.reload();
        } catch (error) {
            save.disabled = false;
            showFlashMessage('Не удалось изменить параметры задачи. Попробуйте ещё раз');
        }
    });
}

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
            showFlashMessage('Ссылка на задачу скопирована');
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
        const count = renameForm.querySelector('[data-settings-name-count]');
        const length = renameForm.querySelector('[data-settings-name-length]');

        setupChangedValueSubmitState(input, submit);

        input.addEventListener('input', function () {
            length.textContent = input.value.length;
            count.hidden = input.value === input.dataset.originalValue;
        });
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
    const tooltip = submit.closest('[data-tooltip]');

    function updateState() {
        const unchanged = input.value === input.dataset.originalValue;

        submit.disabled = unchanged;

        if (tooltip !== null) {
            tooltip.toggleAttribute('data-tooltip-disabled', !unchanged);
        }
    }

    input.addEventListener('input', function () {
        updateState();
    });

    updateState();
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

            if (element.hasAttribute('data-tooltip-disabled')) {
                hideTooltip();
                return;
            }

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
    const closeIcon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    const closeIconPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');

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
    closeIcon.setAttribute('aria-hidden', 'true');
    closeIcon.setAttribute('width', '16');
    closeIcon.setAttribute('height', '16');
    closeIcon.setAttribute('viewBox', '0 0 16 16');
    closeIcon.setAttribute('fill', 'currentColor');
    closeIconPath.setAttribute('d', 'M3.72 3.72a.75.75 0 0 1 1.06 0L8 6.94l3.22-3.22a.749.749 0 0 1 1.275.326.749.749 0 0 1-.215.734L9.06 8l3.22 3.22a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L8 9.06l-3.22 3.22a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042L6.94 8 3.72 4.78a.75.75 0 0 1 0-1.06Z');
    closeIcon.appendChild(closeIconPath);
    closeButton.appendChild(closeIcon);
    flashMessage.append(messageText, closeButton);
    stack.appendChild(flashMessage);

    closeButton.addEventListener('click', function () {
        flashMessage.remove();
    });
    window.setTimeout(function () {
        flashMessage.remove();
    }, 10000);
}
