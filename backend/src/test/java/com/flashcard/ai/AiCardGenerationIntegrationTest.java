package com.flashcard.ai;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.CardRepository;
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
class AiCardGenerationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AiUsageLogRepository aiUsageLogRepository;
    @Autowired private CardRepository cardRepository;

    private AuthPrincipal newUser(Role role, Plan plan) {
        User u = userRepository.save(new User("g-" + UUID.randomUUID() + "@x.com", "h", role, plan));
        return new AuthPrincipal(u.getId(), u.getEmail(), role, plan);
    }

    private RequestPostProcessor as(AuthPrincipal p) {
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role()))));
    }

    private String gen(String text) {
        return "{\"text\":\"" + text + "\"}";
    }

    private List<AiUsageLog> logsFor(AuthPrincipal p) {
        return aiUsageLogRepository.findAll().stream()
                .filter(l -> l.getUserId().equals(p.id())).toList();
    }

    // 7.1 — plan gate
    @Test
    void freeUser_isDenied_403_andNotLogged() throws Exception {
        AuthPrincipal free = newUser(Role.USER, Plan.FREE);
        mockMvc.perform(post("/ai/cards/generate").with(as(free)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(gen("some study text")))
                .andExpect(status().isForbidden());
        assertThat(logsFor(free)).isEmpty();
    }

    // 7.2 — happy path: drafts returned, logged once, no cards created
    @Test
    void premium_generatesDrafts_logsOnce_andCreatesNoCards() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        long cardsBefore = cardRepository.count();

        mockMvc.perform(post("/ai/cards/generate").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(gen("photosynthesis basics")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts.length()").value(2))
                .andExpect(jsonPath("$.drafts[0].front").value("[mock] Q1"))
                .andExpect(jsonPath("$.model").value("mock-1"));

        List<AiUsageLog> logs = logsFor(premium);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getFeatureKey()).isEqualTo("card-generation");
        assertThat(cardRepository.count()).isEqualTo(cardsBefore);
    }

    // 7.3 — input limit
    @Test
    void oversizedInput_400_andNotLogged() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        mockMvc.perform(post("/ai/cards/generate").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(gen("x".repeat(9000))))
                .andExpect(status().isBadRequest());
        assertThat(logsFor(premium)).isEmpty();
    }

    // 7.4 — quota
    @Test
    void overQuota_429() throws Exception {
        AuthPrincipal premium = newUser(Role.USER, Plan.PREMIUM);
        aiUsageLogRepository.save(new AiUsageLog(premium.id(), "seed", 200000, 0, BigDecimal.ZERO));
        mockMvc.perform(post("/ai/cards/generate").with(as(premium)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(gen("hi")))
                .andExpect(status().isTooManyRequests());
    }
}
