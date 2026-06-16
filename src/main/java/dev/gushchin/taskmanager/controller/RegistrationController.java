package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    private final UserService userService;

    @GetMapping("/registration")
    public String registrationPage(Model model, CsrfToken csrfToken) {
        model.addAttribute("_csrf", csrfToken);

        return "registration";
    }

    @PostMapping("/registration")
    public String register(
            @RequestParam String email, @RequestParam(required = false) String name, @RequestParam String password) {
        userService.create(email, name, password);

        return "redirect:/login";
    }
}
