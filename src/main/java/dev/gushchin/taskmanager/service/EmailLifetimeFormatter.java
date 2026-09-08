package dev.gushchin.taskmanager.service;

import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class EmailLifetimeFormatter {
    private static final int TEEN_RANGE_START = 11;
    private static final int TEEN_RANGE_END = 14;
    private static final int LAST_DIGIT_ONE = 1;
    private static final int LAST_DIGIT_FEW_START = 2;
    private static final int LAST_DIGIT_FEW_END = 4;
    private static final int LAST_TWO_DIGITS_MODULO = 100;
    private static final int LAST_DIGIT_MODULO = 10;

    public String formatHours(Duration lifetime) {
        long hours = lifetime.toHours();

        return hours + " " + plural(hours, "час", "часа", "часов");
    }

    public String formatMinutes(Duration lifetime) {
        long minutes = lifetime.toMinutes();

        return minutes + " " + plural(minutes, "минуту", "минуты", "минут");
    }

    public String formatDays(long days) {
        return days + " " + plural(days, "день", "дня", "дней");
    }

    private String plural(long value, String one, String few, String many) {
        long lastTwoDigits = Math.abs(value) % LAST_TWO_DIGITS_MODULO;
        if (lastTwoDigits >= TEEN_RANGE_START && lastTwoDigits <= TEEN_RANGE_END) {
            return many;
        }

        long lastDigit = lastTwoDigits % LAST_DIGIT_MODULO;
        if (lastDigit == LAST_DIGIT_ONE) {
            return one;
        }
        if (lastDigit >= LAST_DIGIT_FEW_START && lastDigit <= LAST_DIGIT_FEW_END) {
            return few;
        }

        return many;
    }
}
