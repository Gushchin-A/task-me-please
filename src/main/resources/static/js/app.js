document.addEventListener('DOMContentLoaded', function () {
    setupProfileMenu();
    setupFlashMessages();
    setupTooltips();
    setupSubmitLoading();
    setupToolbarPopovers();
});

function setupToolbarPopovers() {
    const popovers = document.querySelectorAll('.toolbar-popover, .comment-menu, .team-switcher');

    popovers.forEach(function (popover) {
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
