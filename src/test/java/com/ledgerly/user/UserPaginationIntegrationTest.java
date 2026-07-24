package com.ledgerly.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Transactional
@Import(UserPaginationIntegrationTest.TestConfig.class)
class UserPaginationIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void shouldPaginateFindAll() {
        userService.createUser("pager-1", "password123", "USER");
        userService.createUser("pager-2", "password123", "USER");

        Page<User> page = userService.findAll(PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(1);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
