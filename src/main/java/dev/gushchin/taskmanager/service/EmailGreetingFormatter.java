package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailGreetingFormatter {
    private static final String NEUTRAL_GREETING = "Привет!";

    public String format(User user) {
        String name = user.getName();
        if (!StringUtils.hasText(name) || name.equals(user.getEmail())) {
            return NEUTRAL_GREETING;
        }

        return "Привет, " + name.strip() + "!";
    }
}
