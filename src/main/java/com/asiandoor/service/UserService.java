package com.asiandoor.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.asiandoor.dto.ProfileUpdateRequest;
import com.asiandoor.dto.RegisterRequest;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.RoleRepository;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new buyer account from the registration form.
     *
     * @throws IllegalArgumentException if the email is already in use
     */
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

        return toDTO(userRepository.save(user));
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
