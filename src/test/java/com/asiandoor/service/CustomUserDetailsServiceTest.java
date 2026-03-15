package com.asiandoor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.asiandoor.entity.Role;
import com.asiandoor.entity.User;
import com.asiandoor.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldValidateLoginAndLoadUserDetails() {
        User user = new User();
        user.setEmail("login@test.com");
        user.setPassword("encoded-pass");

        Role role = new Role();
        role.setName("customer");
        user.setRole(role);

        when(userRepository.findByEmail("login@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("login@test.com");

        assertEquals("login@test.com", details.getUsername());
        assertEquals("encoded-pass", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("ROLE_CUSTOMER", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldFailLoginValidationWhenUserNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@test.com"));

        assertEquals("No account found for email: missing@test.com", ex.getMessage());
    }
}
