package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
