package com.medicare.config;

import com.medicare.entity.Role;
import com.medicare.entity.User;
import com.medicare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Solves the chicken-and-egg problem of access control: only an ADMIN
 * can create staff logins, but a brand-new database has no ADMIN yet (and
 * a forgotten admin password can't be read back from its hash).
 *
 * If BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD are set when
 * the application starts, and no user with that username exists, an
 * ADMIN account is created. It NEVER changes or resets an existing
 * account, so leaving the variables set is harmless. The password is
 * never written to the log.
 *
 * Typical use (Windows cmd), then log in and change/reset passwords
 * from the Accounts screen:
 *   set BOOTSTRAP_ADMIN_USERNAME=admin
 *   set BOOTSTRAP_ADMIN_PASSWORD=ChooseSomethingStrong
 *   mvn spring-boot:run
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${bootstrap.admin.username:}") String username,
                                @Value("${bootstrap.admin.password:}") String password,
                                @Value("${bootstrap.admin.email:}") String email) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.email = (email == null || email.isBlank()) ? (this.username + "@medicare.local") : email.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && password.isEmpty()) {
            return;   // feature not in use
        }
        if (username.length() < 3 || password.length() < 6 || password.length() > 72) {
            log.warn("Bootstrap admin NOT created: username must be at least 3 characters "
                    + "and password between 6 and 72 characters");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.info("Bootstrap admin '{}' already exists - left unchanged", username);
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.warn("Bootstrap admin NOT created: email '{}' is already used by another account "
                    + "(set BOOTSTRAP_ADMIN_EMAIL to a different one)", email);
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Bootstrap ADMIN account '{}' created", username);
    }
}
