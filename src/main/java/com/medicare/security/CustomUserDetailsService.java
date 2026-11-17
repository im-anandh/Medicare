package com.medicare.security;

import com.medicare.entity.User;
import com.medicare.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security doesn't know anything about our own User entity -
 * it works with its OWN interface, UserDetails, which just describes
 * "something with a username, a password, and a set of authorities
 * (roles/permissions)." This class's only job is translating our
 * entity into that shape Spring Security understands.
 *
 * UserDetailsService is a Spring Security interface with one method:
 * loadUserByUsername(). We implement it here, and Spring Security
 * calls it automatically during login to check credentials.
 *
 * new SimpleGrantedAuthority("ROLE_" + user.getRole()) - Spring
 * Security's convention is that role-based authority strings are
 * prefixed with "ROLE_". Our Role enum just has ADMIN, DOCTOR, etc.,
 * so we prepend "ROLE_" here to match what hasRole("ADMIN") (used
 * later in SecurityConfig) expects to find.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
