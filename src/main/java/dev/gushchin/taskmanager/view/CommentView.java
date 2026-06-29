package dev.gushchin.taskmanager.view;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record CommentView(
        Long id, String userName, String message, Instant createdAt, Instant updatedAt, boolean canEdit) {
    private static final DateTimeFormatter COMMENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.forLanguageTag("ru"));

    private static final ZoneId COMMENT_TIME_ZONE = ZoneId.of("Europe/Budapest");

    public String createdAtText() {
        return createdAt.atZone(COMMENT_TIME_ZONE).format(COMMENT_TIME_FORMATTER);
    }

    public boolean edited() {
        if (createdAt == null || updatedAt == null) {
            return false;
        }

        return !createdAt.equals(updatedAt);
    }
}
