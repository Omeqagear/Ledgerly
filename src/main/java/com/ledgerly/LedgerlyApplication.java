package com.ledgerly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Ledgerly modular monolith.
 *
 * <p>Spring Modulith discovers modules as direct sub-packages of this base package.
 */
@SpringBootApplication
public class LedgerlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerlyApplication.class, args);
    }
}