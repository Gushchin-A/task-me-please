package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.exception.InvalidPasswordResetRequestException;
import dev.gushchin.taskmanager.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String PASSWORD_RESET_INVALID_VIEW = "reset-password-invalid";
    private static final String PASSWORD_RESET_PATH_PREFIX = "/reset-password/";
    private static final String PASSWORD_RESET_SUCCESS_MESSAGE = "Password изменён. Войдите с новым password.";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";

    private final PasswordResetService passwordResetService;

    @GetMapping(PASSWORD_RESET_PATH_PREFIX + "{token}")
    public String resetPassword(@PathVariable String token, Model model, CsrfToken csrfToken) {
        try {
            passwordResetService.findValid(token);
        } catch (InvalidAccountTokenException ex) {
            return PASSWORD_RESET_INVALID_VIEW;
        }

        model.addAttribute("_csrf", csrfToken);
        model.addAttribute("token", token);
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, getModelAttribute(model, ERROR_MESSAGE_ATTRIBUTE));
        model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, null);
        model.addAttribute("loginUrl", "/login");
        model.addAttribute("registrationUrl", "/registration");

        return "reset-password";
    }

    @PostMapping(PASSWORD_RESET_PATH_PREFIX + "{token}")
    public String resetPassword(
            @PathVariable String token,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String passwordConfirmation,
            RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.reset(token, password, passwordConfirmation);
        } catch (InvalidPasswordResetRequestException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
            return "redirect:" + PASSWORD_RESET_PATH_PREFIX + token;
        } catch (InvalidAccountTokenException ex) {
            return PASSWORD_RESET_INVALID_VIEW;
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, PASSWORD_RESET_SUCCESS_MESSAGE);

        return "redirect:/login";
    }

    private Object getModelAttribute(Model model, String attribute) {
        if (model.containsAttribute(attribute)) {
            return model.getAttribute(attribute);
        }

        return null;
    }
}
