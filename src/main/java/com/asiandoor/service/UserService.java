package com.asiandoor.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.asiandoor.dto.RegisterRequest;
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
    public User registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Role buyerRole = roleRepository.findByName("ROLE_BUYER")
                .orElseThrow(() -> new IllegalStateException("ROLE_BUYER not found — ensure roles are seeded."));

        User user = new User();
        user.setName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(buyerRole);

        return userRepository.save(user);
    }

    /**
     * Looks up a user by their email address.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
