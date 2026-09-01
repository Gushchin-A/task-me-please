package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.NotificationService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.UserAvatarView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserViewAdvice {
    private final NotificationService notificationService;
    private final UserService userService;

    @ModelAttribute("currentUser")
    public UserAvatarView currentUser(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null) {
            return null;
        }

        User user = userService.findById(authUser.getId());

        return UserAvatarView.from(user.getName(), user.getEmail())
                .withUnreadNotifications(
                        notificationService.getCounts(authUser.getId()).unread());
    }
}
