document.addEventListener('DOMContentLoaded', function () {
    const profile = document.querySelector('[data-profile]');

    if (profile === null) {
        return;
    }

    const trigger = profile.querySelector('[data-profile-trigger]');
    const menu = profile.querySelector('[data-profile-menu]');

    function closeProfileMenu() {
        menu.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
    }

    trigger.addEventListener('click', function () {
        const shouldOpen = menu.hidden;

        if (shouldOpen) {
            menu.hidden = false;
            trigger.setAttribute('aria-expanded', 'true');
            return;
        }

        closeProfileMenu();
    });

    document.addEventListener('click', function (event) {
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
});
