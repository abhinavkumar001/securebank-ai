package com.securebank.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates the first admin account on startup if it doesn't exist yet.
 * Customers can self-register; admins can't, so we need one to start with.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordHasher passwordHasher,
                            @Value("${app.admin.email}") String adminEmail,
                            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(new User("Admin", adminEmail, passwordHasher.hash(adminPassword), Role.ADMIN));
            log.info("Created default admin account: {}", adminEmail);
        }
    }
}
