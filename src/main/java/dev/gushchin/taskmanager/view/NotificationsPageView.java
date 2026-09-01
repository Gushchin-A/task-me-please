package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.NotificationSort;

public record NotificationsPageView(
        NotificationPage page,
        NotificationCounts counts,
        boolean unreadOnly,
        NotificationSort sort,
        InvitationDecisionView invitationDecision) {
    private static final int URL_CAPACITY = 64;

    public String invitationUrl(String token) {
        return baseUrl() + separator() + "invitation=" + token;
    }

    public String nextPageUrl() {
        if (page.nextCursor() == null) {
            return null;
        }

        return baseUrl() + separator() + "cursor=" + page.nextCursor();
    }

    private String baseUrl() {
        StringBuilder url = new StringBuilder(URL_CAPACITY).append("/notifications");
        if (unreadOnly) {
            url.append("?unread=true");
        }
        if (sort == NotificationSort.OLDEST) {
            url.append(url.indexOf("?") >= 0 ? "&" : "?").append("sort=OLDEST");
        }

        return url.toString();
    }

    private String separator() {
        return baseUrl().contains("?") ? "&" : "?";
    }
}
