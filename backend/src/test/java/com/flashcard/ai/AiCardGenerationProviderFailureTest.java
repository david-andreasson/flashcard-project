package com.flashcard.ai;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.common.UpstreamAiException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Card generation when the provider misbehaves. The mock {@link AiProvider} bean replaces the
 * real one so we can force junk output (parse failure) or a transport failure — both must surface
 * as HTTP 502 without leaking a 200 with malformed drafts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiCardGenerationProviderFailureTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @MockitoBean private AiProvider aiProvider;

    private RequestPostProcessor premium() {
        User u = userRepository.save(new User("f-" + UUID.randomUUID() + "@x.com", "h", Role.USER, Plan.PREMIUM));
        AuthPrincipal p = new AuthPrincipal(u.getId(), u.getEmail(), Role.USER, Plan.PREMIUM);
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void unparseableProviderOutput_502() throws Exception {
        when(aiProvider.complete(any())).thenReturn(new AiResponse("not json at all", 10, 5, "mock-1"));
        mockMvc.perform(post("/ai/cards/generate").with(premium()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void providerThrows_502() throws Exception {
        when(aiProvider.complete(any())).thenThrow(new UpstreamAiException("boom"));
        mockMvc.perform(post("/ai/cards/generate").with(premium()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isBadGateway());
    }
}
