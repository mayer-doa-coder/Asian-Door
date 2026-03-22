package com.asiandoor.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.asiandoor.entity.User;
import com.asiandoor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security calls this during login using the submitted email as the username.
     * Maps the stored Role name (e.g. ROLE_ADMIN, ROLE_CUSTOMER) to a GrantedAuthority.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for email: " + email));

        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        if (roleName == null || roleName.isBlank()) {
            throw new UsernameNotFoundException("User has no role assigned: " + email);
        }

        String normalizedRole = roleName.trim().toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(normalizedRole);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
            user.isVerified(),
            true,
            true,
            true,
                List.of(authority)
        );
    }
}
