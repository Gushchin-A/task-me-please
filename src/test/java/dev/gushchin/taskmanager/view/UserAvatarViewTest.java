package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserAvatarViewTest {
    @Test
    void fromShouldUseNameAndItsInitial() {
        UserAvatarView avatar = UserAvatarView.from("Анна", "anna@test.com");

        assertEquals("Анна", avatar.displayName());
        assertEquals("А", avatar.initial());
        assertFalse(avatar.emailAsDisplayName());
    }

    @Test
    void fromShouldFallBackToEmailAndItsInitial() {
        UserAvatarView avatar = UserAvatarView.from(" ", "user@test.com");

        assertEquals("user@test.com", avatar.displayName());
        assertEquals("U", avatar.initial());
        assertTrue(avatar.emailAsDisplayName());
    }

    @Test
    void commentDisplayNameShouldTruncateLongNameAndExposeTooltip() {
        String longName = "Илья Соколов с очень длинным именем для проверки переполнения";
        UserAvatarView avatar = UserAvatarView.from(longName, "ilya@test.com");

        assertEquals("Илья Соколов с очень длинным именем д...", avatar.commentDisplayName());
        assertEquals(longName, avatar.commentDisplayNameTooltip());
    }

    @Test
    void commentDisplayNameShouldKeepShortNameWithoutTooltip() {
        UserAvatarView avatar = UserAvatarView.from("Илья Соколов", "ilya@test.com");

        assertEquals("Илья Соколов", avatar.commentDisplayName());
        assertNull(avatar.commentDisplayNameTooltip());
    }
}
