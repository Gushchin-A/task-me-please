package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;

import dev.gushchin.taskmanager.jooq.tables.records.NotificationEventsRecord;
import dev.gushchin.taskmanager.mapper.NotificationEventMapper;
import dev.gushchin.taskmanager.model.NotificationEvent;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationEventRepository {
    private final DSLContext dsl;

    public NotificationEvent save(NotificationEvent event) {
        NotificationEventsRecord record = dsl.insertInto(NOTIFICATION_EVENTS)
                .set(NOTIFICATION_EVENTS.EVENT_TYPE, event.getType().name())
                .set(NOTIFICATION_EVENTS.ACTOR_USER_ID, event.getActorUserId())
                .set(NOTIFICATION_EVENTS.TEAM_ID, event.getTeamId())
                .set(NOTIFICATION_EVENTS.TASK_ID, event.getTaskId())
                .set(NOTIFICATION_EVENTS.COMMENT_ID, event.getCommentId())
                .set(NOTIFICATION_EVENTS.INVITATION_ID, event.getInvitationId())
                .set(NOTIFICATION_EVENTS.SUBJECT_USER_ID, event.getSubjectUserId())
                .set(NOTIFICATION_EVENTS.PAYLOAD, JSON.valueOf(event.getPayload()))
                .set(NOTIFICATION_EVENTS.CREATED_AT, event.getCreatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return NotificationEventMapper.toModel(record);
    }

    public NotificationEvent findById(Long id) {
        NotificationEventsRecord record = dsl.selectFrom(NOTIFICATION_EVENTS)
                .where(NOTIFICATION_EVENTS.ID.eq(id))
                .fetchOne();

        return NotificationEventMapper.toModel(record);
    }
}
