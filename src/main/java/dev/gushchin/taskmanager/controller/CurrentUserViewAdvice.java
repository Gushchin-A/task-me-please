package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.view.UserAvatarView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserViewAdvice {
    @ModelAttribute("currentUser")
    public UserAvatarView currentUser(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null) {
            return null;
        }

        return UserAvatarView.from(authUser.user().getName(), authUser.user().getEmail());
    }
}
