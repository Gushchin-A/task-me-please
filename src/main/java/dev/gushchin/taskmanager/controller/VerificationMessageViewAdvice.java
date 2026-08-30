package dev.gushchin.taskmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class VerificationMessageViewAdvice {
    static final String SESSION_ATTRIBUTE = "verificationSuccessMessage";

    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";

    @ModelAttribute
    public void addVerificationSuccessMessage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object message = session.getAttribute(SESSION_ATTRIBUTE);
        if (message != null) {
            session.removeAttribute(SESSION_ATTRIBUTE);
            if (!model.containsAttribute(SUCCESS_MESSAGE_ATTRIBUTE)) {
                model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, message);
            }
        }
    }
}
