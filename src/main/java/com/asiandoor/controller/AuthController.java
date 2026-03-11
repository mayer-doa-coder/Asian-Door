package com.asiandoor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.asiandoor.dto.RegisterRequest;
import com.asiandoor.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // GET /login  — rendered by Spring Security's filter chain;
    // this method supplies the view for the custom login page.
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // POST /login  — processed entirely by Spring Security (DaoAuthenticationProvider).
    // No controller method needed; Spring Security intercepts it before MVC.

    // GET /logout  — processed entirely by Spring Security's logout filter.
    // No controller method needed.

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           Model model) {
        // Show field-level validation errors (blank fields, invalid email, short password)
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Please correct the highlighted errors and try again.");
            return "register";
        }
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "register";
        }
        try {
            userService.registerUser(registerRequest);
        } catch (IllegalArgumentException e) {
            // Duplicate email
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    // Shown when Spring Security denies access (HTTP 403).
    // Mapped as a forward target by SecurityConfig's AccessDeniedHandler.
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
