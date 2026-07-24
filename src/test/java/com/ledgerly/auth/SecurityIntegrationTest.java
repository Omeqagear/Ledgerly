package com.ledgerly.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/customers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WithValidAdminToken() throws Exception {
        String token = loginAndGetToken("admin", "ledgerly");

        mockMvc.perform(get("/customers")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserCallsUsersEndpoint() throws Exception {
        String username = "testuser-" + UUID.randomUUID();
        String password = "password123";

        String adminToken = loginAndGetToken("admin", "ledgerly");
        createUser(username, password, "USER", adminToken);

        String userToken = loginAndGetToken(username, password);

        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenAdminCallsUsersEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin", "ledgerly");

        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/customers")
                .header("Authorization", "Bearer invalid.token.here"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WithTamperedToken() throws Exception {
        String token = loginAndGetToken("admin", "ledgerly");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        mockMvc.perform(get("/customers")
                .header("Authorization", "Bearer " + tampered))
            .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private void createUser(String username, String password, String role, String adminToken) throws Exception {
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}"))
            .andExpect(status().isCreated());
    }
}
