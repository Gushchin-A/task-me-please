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
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";
    private static final String REQUEST_RESULT_ATTRIBUTE = "requestResult";
    private static final String DELIVERY_FAILED_MESSAGE =
            "Не удалось отправить письмо. Проблема на нашей стороне, мы уже работаем над этим. Попробуйте позже";
    private static final String INVALID_ACCOUNT_MESSAGE =
            "Этот адрес электронной почты недействителен, не подтвержден или не привязан к учетной записи";

    private final PasswordResetRequestService passwordResetRequestService;

    @GetMapping(FORGOT_PASSWORD_PATH)
    public String forgotPassword(Model model, CsrfToken csrfToken) {
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute(REQUEST_RESULT_ATTRIBUTE, getModelAttribute(model, REQUEST_RESULT_ATTRIBUTE));
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, getModelAttribute(model, ERROR_MESSAGE_ATTRIBUTE));
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
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, INVALID_ACCOUNT_MESSAGE);
        }

        if (result == PasswordResetRequestResult.DELIVERY_FAILED) {
            redirectAttributes.addFlashAttribute(EMAIL_ATTRIBUTE, email);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, DELIVERY_FAILED_MESSAGE);
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
