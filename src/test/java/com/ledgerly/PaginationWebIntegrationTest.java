package com.ledgerly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.customer.DuplicateCustomerEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(PaginationWebIntegrationTest.TestConfig.class)
class PaginationWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void seedCustomers() {
        safeCreate("Zulu", "zulu-pag@example.com");
        safeCreate("Alpha", "alpha-pag@example.com");
        safeCreate("Mike", "mike-pag@example.com");
    }

    private void safeCreate(String name, String email) {
        try {
            customerService.createCustomer(name, email, null, null);
        } catch (DuplicateCustomerEmailException ignored) {
        }
    }

    @Test
    void shouldReturnPageJsonWithPagination() throws Exception {
        mockMvc.perform(get("/customers").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").exists())
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.totalPages").isNumber())
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldApplySortQueryParam() throws Exception {
        String response = mockMvc.perform(get("/customers")
                .param("page", "0")
                .param("size", "50")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        List<String> names = new ArrayList<>();
        root.get("content").forEach(node -> names.add(node.get("name").asText()));

        int alphaIndex = names.indexOf("Alpha");
        int mikeIndex = names.indexOf("Mike");
        int zuluIndex = names.indexOf("Zulu");

        assertThat(alphaIndex).isGreaterThan(-1);
        assertThat(mikeIndex).isGreaterThan(-1);
        assertThat(zuluIndex).isGreaterThan(-1);
        assertThat(alphaIndex).isLessThan(mikeIndex);
        assertThat(mikeIndex).isLessThan(zuluIndex);
    }

    @Test
    void shouldClampSizeToMax() throws Exception {
        mockMvc.perform(get("/customers").param("size", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
