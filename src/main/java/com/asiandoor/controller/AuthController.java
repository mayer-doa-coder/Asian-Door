package com.asiandoor.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.asiandoor.dto.RegisterRequest;
import com.asiandoor.service.CustomUserDetailsService;
import com.asiandoor.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

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
                           Model model,
                           HttpSession session) {
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
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Unable to send verification email right now. Please try again.");
            return "register";
        }

        session.setAttribute("pendingVerificationEmail", registerRequest.getEmail());
        return "redirect:/verify-signup";
    }

    @GetMapping("/verify-signup")
    public String verifySignupPage(HttpSession session,
                                   Model model,
                                   @RequestParam(name = "resent", required = false) String resent,
                                   @RequestParam(name = "invalid", required = false) String invalid,
                                   @RequestParam(name = "mailError", required = false) String mailError) {
        Object pendingEmail = session.getAttribute("pendingVerificationEmail");
        if (pendingEmail == null) {
            return "redirect:/register";
        }

        model.addAttribute("email", pendingEmail.toString());
        if (invalid != null) {
            model.addAttribute("errorMessage", "Invalid verification code. Please check and try again.");
        }
        if (resent != null) {
            model.addAttribute("successMessage", "A new verification code has been sent to your email.");
        }
        if (mailError != null) {
            model.addAttribute("errorMessage", "Unable to resend code. Please check email settings and try again.");
        }

        return "verify-signup";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model,
                                     @RequestParam(name = "sent", required = false) String sent,
                                     @RequestParam(name = "error", required = false) String error,
                                     @RequestParam(name = "mailError", required = false) String mailError) {
        if (sent != null) {
            model.addAttribute("successMessage", "A 6-digit reset code has been sent to your email.");
        }
        if (error != null) {
            model.addAttribute("errorMessage", "No account was found with that email address.");
        }
        if (mailError != null) {
            model.addAttribute("errorMessage", "Unable to send reset email right now. Please check mail settings and try again.");
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendForgotPasswordCode(@RequestParam("email") String email, HttpSession session) {
        try {
            boolean sent = userService.requestPasswordResetCode(email);
            if (!sent) {
                return "redirect:/forgot-password?error";
            }
        } catch (Exception e) {
            return "redirect:/forgot-password?mailError";
        }

        session.setAttribute("pendingPasswordResetEmail", email);
        return "redirect:/forgot-password/verify?sent";
    }

    @GetMapping("/forgot-password/verify")
    public String forgotPasswordVerifyPage(HttpSession session, Model model,
                                           @RequestParam(name = "sent", required = false) String sent,
                                           @RequestParam(name = "invalid", required = false) String invalid,
                                           @RequestParam(name = "expired", required = false) String expired,
                                           @RequestParam(name = "resent", required = false) String resent,
                                           @RequestParam(name = "mailError", required = false) String mailError) {
        Object pendingEmail = session.getAttribute("pendingPasswordResetEmail");
        if (pendingEmail == null) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", pendingEmail.toString());
        if (sent != null || resent != null) {
            model.addAttribute("successMessage", "A 6-digit reset code has been sent to your email.");
        }
        if (invalid != null) {
            model.addAttribute("errorMessage", "Invalid reset code. Please check and try again.");
        } else if (expired != null) {
            model.addAttribute("errorMessage", "This reset code has expired. Please request a new code.");
        } else if (mailError != null) {
            model.addAttribute("errorMessage", "Unable to resend reset code right now. Please try again.");
        }
        return "forgot-password-verify";
    }

    @PostMapping("/forgot-password/verify")
    public String forgotPasswordVerifyCode(@RequestParam("code") String code, HttpSession session) {
        Object pendingEmail = session.getAttribute("pendingPasswordResetEmail");
        if (pendingEmail == null) {
            return "redirect:/forgot-password";
        }

        String email = pendingEmail.toString();
        boolean valid = userService.verifyPasswordResetCode(email, code);
        if (!valid) {
            return "redirect:/forgot-password/verify?invalid";
        }

        session.setAttribute("passwordResetVerifiedEmail", email);
        return "redirect:/reset-password";
    }

    @PostMapping("/forgot-password/resend")
    public String resendForgotPasswordCode(HttpSession session) {
        Object pendingEmail = session.getAttribute("pendingPasswordResetEmail");
        if (pendingEmail == null) {
            return "redirect:/forgot-password";
        }

        try {
            userService.requestPasswordResetCode(pendingEmail.toString());
            return "redirect:/forgot-password/verify?resent";
        } catch (Exception e) {
            return "redirect:/forgot-password/verify?mailError";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session, Model model,
                                    @RequestParam(name = "error", required = false) String error) {
        Object verifiedEmail = session.getAttribute("passwordResetVerifiedEmail");
        if (verifiedEmail == null) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", verifiedEmail.toString());
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpSession session) {
        Object verifiedEmail = session.getAttribute("passwordResetVerifiedEmail");
        if (verifiedEmail == null) {
            return "redirect:/forgot-password";
        }

        if (password == null || password.length() < 8) {
            return "redirect:/reset-password?error=Password%20must%20be%20at%20least%208%20characters.";
        }

        if (!password.equals(confirmPassword)) {
            return "redirect:/reset-password?error=Passwords%20do%20not%20match.";
        }

        userService.resetPassword(verifiedEmail.toString(), password);
        session.removeAttribute("pendingPasswordResetEmail");
        session.removeAttribute("passwordResetVerifiedEmail");
        return "redirect:/login?resetSuccess";
    }

    @PostMapping("/verify-signup")
    public String verifySignupCode(@RequestParam("code") String code,
                                   HttpSession session,
                                   HttpServletRequest request) {
        Object pendingEmail = session.getAttribute("pendingVerificationEmail");
        if (pendingEmail == null) {
            return "redirect:/register";
        }

        String email = pendingEmail.toString();
        boolean verified = userService.verifySignupCode(email, code);
        if (!verified) {
            return "redirect:/verify-signup?invalid";
        }

        autoLoginVerifiedUser(email, request);
        session.removeAttribute("pendingVerificationEmail");
        return "redirect:/";
    }

    @PostMapping("/verify-signup/resend")
    public String resendVerificationCode(HttpSession session) {
        Object pendingEmail = session.getAttribute("pendingVerificationEmail");
        if (pendingEmail == null) {
            return "redirect:/register";
        }

        try {
            userService.resendVerificationCode(pendingEmail.toString());
            return "redirect:/verify-signup?resent";
        } catch (Exception e) {
            return "redirect:/verify-signup?mailError";
        }
    }

    private void autoLoginVerifiedUser(String email, HttpServletRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", context);
    }

    // Shown when Spring Security denies access (HTTP 403).
    // Mapped as a forward target by SecurityConfig's AccessDeniedHandler.
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
