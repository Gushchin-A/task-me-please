package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.UserNotificationsRecord;
import dev.gushchin.taskmanager.model.UserNotification;

public final class UserNotificationMapper {
    private UserNotificationMapper() {}

    public static UserNotification toModel(UserNotificationsRecord record) {
        if (record == null) {
            return null;
        }

        return new UserNotification(
                record.getId(),
                record.getNotificationEventId(),
                record.getRecipientUserId(),
                record.getRecipientEmail(),
                record.getReadAt() == null ? null : record.getReadAt().toInstant(),
                record.getCreatedAt().toInstant());
    }
}
