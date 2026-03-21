package com.asiandoor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.asiandoor.dto.RegisterRequest;
import com.asiandoor.dto.ProfileUpdateRequest;
import com.asiandoor.dto.UserDTO;
import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.RoleRepository;
import com.asiandoor.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(userService, "verificationCodeExpiryMinutes", 10);
        ReflectionTestUtils.setField(userService, "passwordResetCodeExpiryMinutes", 10);
        ReflectionTestUtils.setField(userService, "mailFrom", "asianwoodendecor@gmail.com");
        ReflectionTestUtils.setField(userService, "mailFromName", "Asian Wooden Decor Support");

        lenient().when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new java.util.Properties())));
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test Buyer");
        request.setEmail("buyer@test.com");
        request.setPassword("raw-password");
        request.setConfirmPassword("raw-password");

        Role customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setName("ROLE_CUSTOMER");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(77L);
            return saved;
        });

        UserDTO result = userService.registerUser(request);

        assertNotNull(result);
        assertEquals(77L, result.getId());
        assertEquals("Test Buyer", result.getName());
        assertEquals("buyer@test.com", result.getEmail());
        assertEquals("ROLE_CUSTOMER", result.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals("encoded-password", persisted.getPassword());
        assertEquals(customerRole, persisted.getRole());
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@test.com");

        User existing = new User();
        existing.setEmail("exists@test.com");

        when(userRepository.findByEmail("exists@test.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(request));

        assertEquals("Email is already registered.", ex.getMessage());
    }

    @Test
    void shouldFailRegistrationWhenCustomerRoleMissing() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("No Role");
        request.setEmail("norole@test.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("norole@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.registerUser(request));

        assertEquals("ROLE_CUSTOMER not found — ensure roles are seeded.", ex.getMessage());
    }

    @Test
    void shouldUpdateProfileWithOptionalPassword() {
        User current = new User();
        current.setId(11L);
        current.setName("Old Name");
        current.setEmail("old@test.com");
        current.setPassword("old-hash");

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("New Name");
        request.setEmail("new@test.com");
        request.setPassword("");

        when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.updateProfile("old@test.com", request);

        assertEquals("New Name", result.getName());
        assertEquals("new@test.com", result.getEmail());
        assertEquals("old-hash", current.getPassword());
    }

    @Test
    void shouldRejectDuplicateEmailOnProfileUpdate() {
        User current = new User();
        current.setId(11L);
        current.setEmail("old@test.com");

        User duplicate = new User();
        duplicate.setId(22L);
        duplicate.setEmail("new@test.com");

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("User");
        request.setEmail("new@test.com");

        when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(duplicate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile("old@test.com", request));

        assertEquals("Email is already registered.", ex.getMessage());
    }

    @Test
    void shouldEncodePasswordWhenProvidedInProfileUpdate() {
        User current = new User();
        current.setId(11L);
        current.setName("Buyer");
        current.setEmail("buyer@test.com");
        current.setPassword("old-hash");

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Buyer");
        request.setEmail("buyer@test.com");
        request.setPassword("new-pass-123");
        request.setConfirmPassword("new-pass-123");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(current));
        when(passwordEncoder.encode("new-pass-123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateProfile("buyer@test.com", request);

        assertTrue("new-hash".equals(current.getPassword()));
    }

    @Test
    void shouldRejectPasswordConfirmationMismatch() {
        User current = new User();
        current.setId(11L);
        current.setName("Buyer");
        current.setEmail("buyer@test.com");

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Buyer");
        request.setEmail("buyer@test.com");
        request.setPassword("new-pass-123");
        request.setConfirmPassword("wrong-confirm");

        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(current));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile("buyer@test.com", request));

        assertEquals("Password and retype password do not match.", ex.getMessage());
    }
}
