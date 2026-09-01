package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;

import dev.gushchin.taskmanager.jooq.tables.records.NotificationEventsRecord;
import dev.gushchin.taskmanager.jooq.tables.records.UserNotificationsRecord;
import dev.gushchin.taskmanager.mapper.NotificationEventMapper;
import dev.gushchin.taskmanager.mapper.UserNotificationMapper;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.UserNotification;
import dev.gushchin.taskmanager.view.NotificationFeedItem;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserNotificationRepository {
    private final DSLContext dsl;

    public UserNotification save(UserNotification notification) {
        UserNotificationsRecord record = dsl.insertInto(USER_NOTIFICATIONS)
                .set(USER_NOTIFICATIONS.NOTIFICATION_EVENT_ID, notification.getNotificationEventId())
                .set(USER_NOTIFICATIONS.RECIPIENT_USER_ID, notification.getRecipientUserId())
                .set(USER_NOTIFICATIONS.RECIPIENT_EMAIL, notification.getRecipientEmail())
                .set(
                        USER_NOTIFICATIONS.READ_AT,
                        notification.getReadAt() == null
                                ? null
                                : notification.getReadAt().atOffset(ZoneOffset.UTC))
                .set(USER_NOTIFICATIONS.CREATED_AT, notification.getCreatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return UserNotificationMapper.toModel(record);
    }

    public List<UserNotification> findByUserId(UUID userId) {
        return dsl.selectFrom(USER_NOTIFICATIONS)
                .where(USER_NOTIFICATIONS.RECIPIENT_USER_ID.eq(userId))
                .orderBy(USER_NOTIFICATIONS.ID.desc())
                .fetch()
                .map(UserNotificationMapper::toModel);
    }

    public List<NotificationFeedItem> findPage(
            UUID userId, boolean unreadOnly, NotificationSort sort, Long cursor, int limit) {
        Condition condition = USER_NOTIFICATIONS.RECIPIENT_USER_ID.eq(userId);

        if (unreadOnly) {
            condition = condition.and(USER_NOTIFICATIONS.READ_AT.isNull());
        }

        if (cursor != null && sort == NotificationSort.NEWEST) {
            condition = condition.and(USER_NOTIFICATIONS.ID.lt(cursor));
        } else if (cursor != null) {
            condition = condition.and(USER_NOTIFICATIONS.ID.gt(cursor));
        }

        List<Record2<UserNotificationsRecord, NotificationEventsRecord>> records = dsl.select(
                        USER_NOTIFICATIONS, NOTIFICATION_EVENTS)
                .from(USER_NOTIFICATIONS)
                .join(NOTIFICATION_EVENTS)
                .on(NOTIFICATION_EVENTS.ID.eq(USER_NOTIFICATIONS.NOTIFICATION_EVENT_ID))
                .where(condition)
                .orderBy(sort == NotificationSort.NEWEST ? USER_NOTIFICATIONS.ID.desc() : USER_NOTIFICATIONS.ID.asc())
                .limit(limit)
                .fetch();

        return records.stream()
                .map(record -> new NotificationFeedItem(
                        UserNotificationMapper.toModel(record.value1()),
                        NotificationEventMapper.toModel(record.value2())))
                .toList();
    }

    public int countByUserId(UUID userId, boolean unreadOnly) {
        Condition condition = USER_NOTIFICATIONS.RECIPIENT_USER_ID.eq(userId);
        if (unreadOnly) {
            condition = condition.and(USER_NOTIFICATIONS.READ_AT.isNull());
        }

        return dsl.fetchCount(dsl.selectFrom(USER_NOTIFICATIONS).where(condition));
    }

    public int markAsRead(UUID userId, List<Long> ids, Instant readAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        return dsl.update(USER_NOTIFICATIONS)
                .set(USER_NOTIFICATIONS.READ_AT, readAt.atOffset(ZoneOffset.UTC))
                .where(USER_NOTIFICATIONS.RECIPIENT_USER_ID.eq(userId))
                .and(USER_NOTIFICATIONS.ID.in(ids))
                .and(USER_NOTIFICATIONS.READ_AT.isNull())
                .execute();
    }

    public int claimByEmail(UUID userId, String normalizedEmail) {
        return dsl.update(USER_NOTIFICATIONS)
                .set(USER_NOTIFICATIONS.RECIPIENT_USER_ID, userId)
                .setNull(USER_NOTIFICATIONS.RECIPIENT_EMAIL)
                .where(USER_NOTIFICATIONS.RECIPIENT_USER_ID.isNull())
                .and(USER_NOTIFICATIONS.RECIPIENT_EMAIL.eq(normalizedEmail))
                .execute();
    }
}
