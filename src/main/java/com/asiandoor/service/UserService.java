package com.asiandoor.service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.ProfileUpdateRequest;
import com.asiandoor.dto.RegisterRequest;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.RoleRepository;
import com.asiandoor.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.verification.code-expiry-minutes:10}")
    private int verificationCodeExpiryMinutes;

    @Value("${app.password-reset.code-expiry-minutes:10}")
    private int passwordResetCodeExpiryMinutes;

    @Value("${app.mail.from-email:no-reply@asiandoor.local}")
    private String mailFrom;

    @Value("${app.mail.from-name:Asian Door}")
    private String mailFromName;

    private static final Random RANDOM = new Random();

    /**
     * Registers a new buyer account from the registration form.
     *
     * @throws IllegalArgumentException if the email is already in use
     */
    @Transactional
    public UserDTO registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Role buyerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER not found — ensure roles are seeded."));

        User user = new User();
        user.setName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(buyerRole);
        user.setVerified(false);

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(verificationCodeExpiryMinutes));

        User savedUser = userRepository.save(user);
        try {
            sendVerificationEmail(savedUser.getEmail(), verificationCode);
        } catch (MailAuthenticationException | MailSendException e) {
            throw new IllegalStateException(
                    "Email service is not configured. Set MAIL_USERNAME and MAIL_PASSWORD (App Password), then try again.",
                    e
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to send verification email right now. Please try again.", e);
        }

        return toDTO(savedUser);
    }

    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.isVerified()) {
            return;
        }

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(verificationCodeExpiryMinutes));
        userRepository.save(user);
        sendVerificationEmail(user.getEmail(), verificationCode);
    }

    public boolean verifySignupCode(String email, String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        Optional<User> userOptional = userRepository.findByEmailAndVerificationCode(email, code.trim());
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        if (user.isVerified()) {
            return true;
        }

        if (user.getVerificationCodeExpiresAt() == null || LocalDateTime.now().isAfter(user.getVerificationCodeExpiresAt())) {
            return false;
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    public boolean requestPasswordResetCode(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        String resetCode = generateVerificationCode();
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(passwordResetCodeExpiryMinutes));
        userRepository.save(user);

        try {
            sendPasswordResetEmail(user.getEmail(), resetCode);
        } catch (MailAuthenticationException | MailSendException e) {
            throw new IllegalStateException(
                    "Email service is not configured. Set MAIL_USERNAME and MAIL_PASSWORD (App Password), then try again.",
                    e
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to send reset email right now. Please try again.", e);
        }

        return true;
    }

    public boolean verifyPasswordResetCode(String email, String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        Optional<User> userOptional = userRepository.findByEmailAndPasswordResetCode(email, code.trim());
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        if (user.getPasswordResetCodeExpiresAt() == null || LocalDateTime.now().isAfter(user.getPasswordResetCodeExpiresAt())) {
            return false;
        }

        return true;
    }

    public boolean resetPassword(String email, String newPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    private String generateVerificationCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(value);
    }

    private void sendVerificationEmail(String recipient, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(recipient);
            helper.setSubject("Asian Door verification code");
            helper.setText(
                    "Welcome to Asian Door!\n\n"
                            + "Your 6-digit verification code is: " + code + "\n"
                            + "This code expires in " + verificationCodeExpiryMinutes + " minutes.\n\n"
                            + "If you did not create this account, you can ignore this email."
            );
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to compose verification email.", e);
        }
    }

    private void sendPasswordResetEmail(String recipient, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(recipient);
            helper.setSubject("Asian Door password reset code");
            helper.setText(
                    "We received a request to reset your Asian Door password.\n\n"
                            + "Your 6-digit reset code is: " + code + "\n"
                            + "This code expires in " + passwordResetCodeExpiryMinutes + " minutes.\n\n"
                            + "If you did not request this, you can ignore this email."
            );
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to compose password reset email.", e);
        }
    }

    public Optional<UserDTO> findUserDTOByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDTO);
    }

    public UserDTO updateProfile(String currentEmail, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        String updatedName = request.getName() != null ? request.getName().trim() : null;
        String updatedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;

        if (!StringUtils.hasText(updatedName)) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (!StringUtils.hasText(updatedEmail)) {
            throw new IllegalArgumentException("Email is required.");
        }

        userRepository.findByEmail(updatedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email is already registered.");
                });

        user.setName(updatedName);
        user.setEmail(updatedEmail);

        if (StringUtils.hasText(request.getPassword())) {
            String password = request.getPassword().trim();
            String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";
            if (password.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Password and retype password do not match.");
            }
            user.setPassword(passwordEncoder.encode(password));
        } else if (StringUtils.hasText(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Enter a new password before retyping it.");
        }

        return toDTO(userRepository.save(user));
    }

    public UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().getName() : null);
        return dto;
    }
}
