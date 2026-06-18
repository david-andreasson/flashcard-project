package com.flashcard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String body(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("email", email);
            put("password", password);
        }});
    }

    @Test
    void fullFlow_register_me_refresh_logout_thenOldTokenRejected() throws Exception {
        String email = uniqueEmail();

        // Register → 201, sets access + refresh cookies, auto-login.
        MvcResult registered = mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        Cookie access = registered.getResponse().getCookie("access_token");
        Cookie refresh = registered.getResponse().getCookie("refresh_token");
        assertThat(access).isNotNull();
        assertThat(refresh).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();

        // GET /auth/me with the access cookie → 200.
        mockMvc.perform(get("/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Refresh rotates the token: new refresh cookie, different value.
        MvcResult refreshed = mockMvc.perform(post("/auth/refresh").with(csrf()).cookie(refresh))
                .andExpect(status().isOk())
                .andReturn();
        Cookie newAccess = refreshed.getResponse().getCookie("access_token");
        Cookie newRefresh = refreshed.getResponse().getCookie("refresh_token");
        assertThat(newAccess).isNotNull();
        assertThat(newRefresh).isNotNull();
        assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());

        // The original refresh token was rotated away → 401.
        mockMvc.perform(post("/auth/refresh").with(csrf()).cookie(refresh))
                .andExpect(status().isUnauthorized());

        // Logout revokes the (new) refresh token (requires the access cookie too).
        mockMvc.perform(post("/auth/logout").with(csrf()).cookie(newAccess, newRefresh))
                .andExpect(status().isNoContent());

        // After logout the refresh token no longer works → 401.
        mockMvc.perform(post("/auth/refresh").with(csrf()).cookie(newRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequest_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueEmail(), "password123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "password123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withConfiguredAdminEmail_grantsAdminRole() throws Exception {
        // application.yml default app.admin-email is davidandreasson@live.com.
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("davidandreasson@live.com", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "password123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "password123")))
                .andExpect(status().isConflict());
    }
}
