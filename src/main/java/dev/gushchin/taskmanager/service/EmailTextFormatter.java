package dev.gushchin.taskmanager.service;

import java.util.regex.Pattern;

public final class EmailTextFormatter {
    private static final Pattern SINGLE_LETTER_WORD = Pattern.compile("(?iu)(?<!\\S)([вкиоусаи])\\s+(?=\\p{L})");
    private static final String NON_BREAKING_SPACE = "\u00A0";

    private EmailTextFormatter() {}

    public static String format(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return SINGLE_LETTER_WORD.matcher(text).replaceAll("$1" + NON_BREAKING_SPACE);
    }
}
