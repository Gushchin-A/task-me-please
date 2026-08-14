package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.PasswordResetRequestResult;
import dev.gushchin.taskmanager.service.PasswordResetRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetRequestController {
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    private static final String REQUEST_RESULT_ATTRIBUTE = "requestResult";

    private final PasswordResetRequestService passwordResetRequestService;

    @GetMapping(FORGOT_PASSWORD_PATH)
    public String forgotPassword(Model model, CsrfToken csrfToken) {
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute(REQUEST_RESULT_ATTRIBUTE, getModelAttribute(model, REQUEST_RESULT_ATTRIBUTE));
        model.addAttribute("errorMessage", null);
        model.addAttribute("successMessage", null);
        model.addAttribute("loginUrl", "/login");
        model.addAttribute("registrationUrl", "/registration");
        model.addAttribute(EMAIL_ATTRIBUTE, getModelAttribute(model, EMAIL_ATTRIBUTE));

        return "forgot-password";
    }

    @PostMapping(FORGOT_PASSWORD_PATH)
    public String requestPasswordReset(
            @RequestParam(required = false) String email, RedirectAttributes redirectAttributes) {
        PasswordResetRequestResult result = passwordResetRequestService.request(email);
        redirectAttributes.addFlashAttribute(REQUEST_RESULT_ATTRIBUTE, result);
        if (result == PasswordResetRequestResult.INVALID_ACCOUNT) {
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, email);
        }

        return "redirect:" + FORGOT_PASSWORD_PATH;
    }

    private Object getModelAttribute(Model model, String attribute) {
        if (model.containsAttribute(attribute)) {
            return model.getAttribute(attribute);
        }

        return null;
    }
}
