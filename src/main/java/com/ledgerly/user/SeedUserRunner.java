package com.ledgerly.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedUserRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUserRunner.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public SeedUserRunner(UserService userService, PasswordEncoder passwordEncoder,
                          @Value("${ledgerly.seed.admin-username:admin}") String adminUsername,
                          @Value("${ledgerly.seed.admin-password:ledgerly}") String adminPassword) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userService.findByUsername(adminUsername).isEmpty()) {
            userService.createUser(adminUsername, adminPassword, "ADMIN");
            log.info("Seeded admin user: {}", adminUsername);
        }
    }
}
