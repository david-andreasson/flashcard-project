package com.flashcard.ai;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.user.Plan;
import com.flashcard.user.Role;
import com.flashcard.user.User;
import com.flashcard.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiPipelineIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AiUsageLogRepository aiUsageLogRepository;

    private AuthPrincipal newUser(Role role, Plan plan) {
        User u = userRepository.save(new User("u-" + UUID.randomUUID() + "@x.com", "h", role, plan));
        return new AuthPrincipal(u.getId(), u.getEmail(), role, plan);
    }

    private RequestPostProcessor as(AuthPrincipal p) {
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role()))));
    }

    private String echo(String message) {
        return "{\"message\":\"" + message + "\"}";
    }

    private List<AiUsageLog> logsFor(AuthPrincipal p) {
        return aiUsageLogRepository.findAll().stream()
                .filter(l -> l.getUserId().equals(p.id())).toList();
    }

    // 6.2 — plan gate
    @Test
    void planGate_freeDenied_premiumAndAdminAllowed() throws Exception {
        AuthPrincipal free = newUser(Role.USER, Plan.FREE);
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        AuthPrincipal admin = newUser(Role.ADMIN, Plan.FREE);

        mockMvc.perform(post("/ai/echo").with(as(free)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hi")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/ai/echo").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("[mock] hi"))
                .andExpect(jsonPath("$.outputTokens").isNumber());

        mockMvc.perform(post("/ai/echo").with(as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hi")))
                .andExpect(status().isOk());
    }

    // 6.3 — input limit (max-input-chars = 24000 since change 09)
    @Test
    void inputLimit_oversized_400() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        String big = "x".repeat(25000);
        mockMvc.perform(post("/ai/echo").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo(big)))
                .andExpect(status().isBadRequest());
        assertThat(logsFor(premium)).isEmpty(); // rejected before the provider call
    }

    // 6.4 — quota (default premium limit = 200000)
    @Test
    void quota_overLimit_429_underLimit_grows() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);

        // a fresh user is under quota → 200
        mockMvc.perform(post("/ai/echo").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hi")))
                .andExpect(status().isOk());
        assertThat(logsFor(premium)).hasSize(1);

        // seed usage up to the limit → next call 429
        aiUsageLogRepository.save(new AiUsageLog(premium.id(), "seed", 200000, 0, BigDecimal.ZERO));
        mockMvc.perform(post("/ai/echo").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hi")))
                .andExpect(status().isTooManyRequests());
    }

    // 6.5 — logging
    @Test
    void successfulCall_writesUsageLog() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        mockMvc.perform(post("/ai/echo").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(echo("hello")))
                .andExpect(status().isOk());

        List<AiUsageLog> logs = logsFor(premium);
        assertThat(logs).hasSize(1);
        AiUsageLog log = logs.get(0);
        assertThat(log.getFeatureKey()).isEqualTo("echo");
        assertThat(log.getInputTokens()).isPositive();
        assertThat(log.getOutputTokens()).isPositive();
        assertThat(log.getEstimatedCostUsd()).isNotNull();
    }

    // 6.6 — month-to-date summation excludes rows outside the window
    @Test
    void monthlySummation_excludesRowsBeforeTheWindow() {
        AuthPrincipal u = newUser(Role.USER, Plan.PREMIUM);
        aiUsageLogRepository.save(new AiUsageLog(u.id(), "f", 100, 50, BigDecimal.ZERO));  // 150
        aiUsageLogRepository.save(new AiUsageLog(u.id(), "f", 200, 100, BigDecimal.ZERO)); // 300
        Instant now = Instant.now();
        assertThat(aiUsageLogRepository.sumTokensSince(u.id(), now.minusSeconds(120))).isEqualTo(450);
        assertThat(aiUsageLogRepository.sumTokensSince(u.id(), now.plusSeconds(120))).isEqualTo(0);
    }
}
