package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskParticipantViewTest {
    @Test
    void commentDisplayNameShouldTruncateLongNameAndExposeTooltip() {
        String longName = "Илья Соколов с очень длинным именем для проверки переполнения";
        TaskParticipantView participant = new TaskParticipantView(UUID.randomUUID(), longName, false);

        assertEquals("Илья Соколов с очень длинным именем д...", participant.commentDisplayName());
        assertEquals(longName, participant.commentDisplayNameTooltip());
    }

    @Test
    void commentDisplayNameShouldKeepShortNameWithoutTooltip() {
        TaskParticipantView participant = new TaskParticipantView(UUID.randomUUID(), "Илья Соколов", false);

        assertEquals("Илья Соколов", participant.commentDisplayName());
        assertNull(participant.commentDisplayNameTooltip());
    }

    @Test
    void filterDisplayNameShouldTruncateAfterFifteenCharactersAndExposeTooltip() {
        String longName = "Илья Соколов с очень длинным именем";
        TaskParticipantView participant = new TaskParticipantView(UUID.randomUUID(), longName, false);

        assertEquals("Илья Соколов с ...", participant.filterDisplayName());
        assertEquals(longName, participant.filterDisplayNameTooltip());
    }

    @Test
    void filterDisplayNameShouldKeepFifteenCharactersWithoutTooltip() {
        TaskParticipantView participant = new TaskParticipantView(UUID.randomUUID(), "123456789012345", false);

        assertEquals("123456789012345", participant.filterDisplayName());
        assertNull(participant.filterDisplayNameTooltip());
    }
}
