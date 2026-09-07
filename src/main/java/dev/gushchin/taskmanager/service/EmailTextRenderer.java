package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.view.EmailContentView;
import dev.gushchin.taskmanager.view.EmailParameterView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailTextRenderer {
    private static final String BLOCK_SEPARATOR = "\n\n";
    private static final String LINE_SEPARATOR = "\n";

    public String render(EmailContentView content) {
        List<String> blocks = new ArrayList<>(content.getBodyParagraphs());

        if (content.hasParameters()) {
            blocks.add(renderParameters(content.getParameters()));
        }
        if (content.hasAction()) {
            blocks.add(content.getAction().label() + ": " + content.getAction().url());
        }
        if (content.hasNotes()) {
            blocks.add(String.join(LINE_SEPARATOR, content.getNotes()));
        }

        return String.join(BLOCK_SEPARATOR, blocks);
    }

    private String renderParameters(List<EmailParameterView> parameters) {
        List<String> lines = new ArrayList<>();
        for (EmailParameterView parameter : parameters) {
            lines.add(parameter.label() + ": " + parameter.value());
        }

        return String.join(LINE_SEPARATOR, lines);
    }
}
