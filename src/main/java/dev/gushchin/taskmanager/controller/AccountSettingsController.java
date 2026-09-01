package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.AccountService;
import dev.gushchin.taskmanager.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AccountSettingsController {
    private static final String DELETE_ACCOUNT_CONFIRMATION_TEXT = "я хочу удалить аккаунт";
    private static final String DELETE_ACCOUNT_AND_TEAMS_CONFIRMATION_TEXT = "я хочу удалить аккаунт и команды";
    private static final String DELETE_SECTION = "delete";
    private static final String PROFILE_SECTION = "profile";
    private static final String REDIRECT_SETTINGS = "redirect:/settings";

    private final AccountService accountService;
    private final UserService userService;

    @GetMapping("/settings")
    public String page(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = PROFILE_SECTION) String section,
            Model model,
            CsrfToken csrfToken) {
        String settingsSection = DELETE_SECTION.equals(section) ? DELETE_SECTION : PROFILE_SECTION;
        User user = userService.findById(authUser.getId());

        model.addAttribute("user", user);
        model.addAttribute("settingsSection", settingsSection);
        model.addAttribute("hasOwnedTeams", accountService.hasOwnedTeams(authUser.getId()));
        model.addAttribute("_csrf", csrfToken);

        return "settings/index";
    }

    @PostMapping("/settings/name")
    public String updateName(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        userService.updateName(authUser.getId(), name);
        redirectAttributes.addFlashAttribute("successMessage", "Имя успешно изменено");

        return REDIRECT_SETTINGS;
    }

    @PostMapping("/settings/delete")
    public String deleteAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "") String confirmationText,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        String expectedConfirmationText = accountService.hasOwnedTeams(authUser.getId())
                ? DELETE_ACCOUNT_AND_TEAMS_CONFIRMATION_TEXT
                : DELETE_ACCOUNT_CONFIRMATION_TEXT;

        if (!expectedConfirmationText.equals(confirmationText)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Проверочный текст введен неверно");
            return REDIRECT_SETTINGS + "?section=delete";
        }

        accountService.delete(authUser.getId());
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());

        return "redirect:/login";
    }
}
