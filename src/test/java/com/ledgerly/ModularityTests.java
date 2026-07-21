package com.ledgerly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(LedgerlyApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void printModuleOverview() {
        modules.forEach(System.out::println);
    }
}