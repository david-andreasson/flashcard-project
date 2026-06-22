package com.flashcard.pdf;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.user.Plan;
import com.flashcard.user.Role;
import com.flashcard.user.User;
import com.flashcard.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfExtractionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private RequestPostProcessor freeUser() {
        User u = userRepository.save(new User("pdf-" + UUID.randomUUID() + "@x.com", "h", Role.USER, Plan.FREE));
        AuthPrincipal p = new AuthPrincipal(u.getId(), u.getEmail(), Role.USER, Plan.FREE);
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    // A FREE user (no PREMIUM) can extract — extraction makes no AI call, so it is not plan-gated.
    @Test
    void freeUser_extractsTextPdf_200() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", PdfExtractionServiceTest.pdfWithText("Mitochondria"));
        mockMvc.perform(multipart("/ai/cards/extract-pdf").file(pdf).with(freeUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", containsString("Mitochondria")))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void nonPdf_rejected_400() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/ai/cards/extract-pdf").file(txt).with(freeUser()).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
