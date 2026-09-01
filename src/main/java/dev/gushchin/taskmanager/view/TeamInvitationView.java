package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import java.time.Duration;
import java.time.Instant;

public record TeamInvitationView(
        Long id, String invitedEmail, TeamInvitationStatus status, String invitationUrl, Instant createdAt) {
    private static final int DAYS_PER_TWO_DAYS = 2;
    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINIMUM_ELAPSED_UNIT = 1;
    private static final int PLURAL_BASE = 100;
    private static final int PLURAL_FEW_END = 4;
    private static final int PLURAL_FEW_START = 2;
    private static final int PLURAL_LAST_DIGIT_BASE = 10;
    private static final int PLURAL_TEEN_END = 14;
    private static final int PLURAL_TEEN_START = 11;
    private static final String INVITATION_PATH_PREFIX = "/invitations/";

    public static TeamInvitationView from(TeamInvitation invitation) {
        String invitationUrl = null;

        if (invitation.getStatus() == TeamInvitationStatus.PENDING) {
            invitationUrl = INVITATION_PATH_PREFIX + invitation.getToken();
        }

        return new TeamInvitationView(
                invitation.getId(),
                invitation.getInvitedEmail(),
                invitation.getStatus(),
                invitationUrl,
                invitation.getCreatedAt());
    }

    public boolean canCancel() {
        return status == TeamInvitationStatus.PENDING;
    }

    public String statusText() {
        return switch (status) {
            case PENDING -> "Ожидание";
            case ACCEPTED -> "Принято";
            case DECLINED -> "Отклонено";
            case CANCELED -> "Отменено";
            case EXPIRED -> "Истекло";
        };
    }

    public String statusTooltip() {
        return switch (status) {
            case PENDING -> "Скопируйте ссылку и отправьте ее лично";
            case ACCEPTED -> "Пользователь принял приглашение";
            case DECLINED -> "Пользователь отклонил приглашение";
            case CANCELED -> "Вы отменили приглашение";
            case EXPIRED -> "Срок действия ссылки истек";
        };
    }

    public String statusCssClass() {
        return switch (status) {
            case PENDING -> "pending";
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
            case CANCELED -> "canceled";
            case EXPIRED -> "expired";
        };
    }

    public String createdAtText() {
        if (createdAt == null) {
            return "";
        }

        Duration elapsed = Duration.between(createdAt, Instant.now());
        long minutes = Math.max(0, elapsed.toMinutes());
        long hours = elapsed.toHours();
        String createdAtText;

        if (minutes < MINIMUM_ELAPSED_UNIT) {
            createdAtText = "только что";
        } else if (minutes < MINUTES_PER_HOUR) {
            createdAtText = formatRelativeTime(minutes, "минуту", "минуты", "минут");
        } else if (hours < HOURS_PER_DAY) {
            createdAtText = formatRelativeTime(hours, "час", "часа", "часов");
        } else if (hours < HOURS_PER_DAY * DAYS_PER_TWO_DAYS) {
            createdAtText = "вчера";
        } else {
            createdAtText = formatRelativeTime(elapsed.toDays(), "день", "дня", "дней");
        }

        return createdAtText;
    }

    private static String formatRelativeTime(long value, String singular, String few, String many) {
        long lastTwoDigits = value % PLURAL_BASE;
        long lastDigit = value % PLURAL_LAST_DIGIT_BASE;
        String unit = many;

        if (lastTwoDigits < PLURAL_TEEN_START || lastTwoDigits > PLURAL_TEEN_END) {
            if (lastDigit == MINIMUM_ELAPSED_UNIT) {
                unit = singular;
            } else if (lastDigit >= PLURAL_FEW_START && lastDigit <= PLURAL_FEW_END) {
                unit = few;
            }
        }

        return value + " " + unit + " назад";
    }
}
