package dev.gushchin.taskmanager.view;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Builder
@Getter
public class EmailContentView {
    private static final String BLOCK_MARGIN = "20px";
    private static final String NO_MARGIN = "0";

    private final String heading;

    @Singular
    private final List<String> bodyParagraphs;

    @Singular
    private final List<EmailParameterView> parameters;

    private final EmailActionView action;

    @Singular
    private final List<String> notes;

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public boolean hasAction() {
        return action != null;
    }

    public boolean hasNotes() {
        return !notes.isEmpty();
    }

    public String bodyMarginBottom() {
        return hasParameters() || hasAction() ? BLOCK_MARGIN : NO_MARGIN;
    }

    public String parametersMarginBottom() {
        return hasAction() ? BLOCK_MARGIN : NO_MARGIN;
    }
}
