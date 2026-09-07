package dev.gushchin.taskmanager.service;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTemplateRenderer {
    private final TemplateEngine templateEngine;

    public String render(String template, Map<String, Object> parameters) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, parameters, output);

        return output.toString();
    }
}
