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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "ai.enabled=false")
class AiKillSwitchTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AiUsageLogRepository aiUsageLogRepository;

    // 6.1 — kill-switch blocks all AI with 503 and logs nothing
    @Test
    void killSwitch_blocksAi_503_andLogsNothing() throws Exception {
        User u = userRepository.save(new User("k-" + UUID.randomUUID() + "@x.com", "h", Role.USER, Plan.PREMIUM));
        AuthPrincipal premium = new AuthPrincipal(u.getId(), u.getEmail(), Role.USER, Plan.PREMIUM);
        RequestPostProcessor as = authentication(new UsernamePasswordAuthenticationToken(
                premium, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        mockMvc.perform(post("/ai/echo").with(as).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isServiceUnavailable());

        assertThat(aiUsageLogRepository.findAll().stream()
                .anyMatch(l -> l.getUserId().equals(u.getId()))).isFalse();
    }
}
