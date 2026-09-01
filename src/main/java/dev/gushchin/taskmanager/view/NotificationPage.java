package dev.gushchin.taskmanager.view;

import java.util.List;

public record NotificationPage(List<NotificationFeedItem> items, Long nextCursor) {}
