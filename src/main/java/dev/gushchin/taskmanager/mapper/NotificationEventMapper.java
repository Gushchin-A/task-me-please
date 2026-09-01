package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.NotificationEventsRecord;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventType;

public final class NotificationEventMapper {
    private NotificationEventMapper() {}

    public static NotificationEvent toModel(NotificationEventsRecord record) {
        if (record == null) {
            return null;
        }

        return new NotificationEvent(
                record.getId(),
                NotificationEventType.valueOf(record.getEventType()),
                record.getActorUserId(),
                record.getTeamId(),
                record.getTaskId(),
                record.getCommentId(),
                record.getInvitationId(),
                record.getSubjectUserId(),
                record.getPayload().data(),
                record.getCreatedAt().toInstant());
    }
}
