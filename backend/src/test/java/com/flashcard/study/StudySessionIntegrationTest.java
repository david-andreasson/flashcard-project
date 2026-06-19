package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.course.Course;
import com.flashcard.course.CourseRepository;
import com.flashcard.course.Visibility;
import com.flashcard.deck.Deck;
import com.flashcard.deck.DeckRepository;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudySessionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DeckRepository deckRepository;

    private AuthPrincipal newUser() {
        User u = userRepository.save(new User("u-" + UUID.randomUUID() + "@x.com", "h", Role.USER, Plan.FREE));
        return new AuthPrincipal(u.getId(), u.getEmail(), Role.USER, u.getPlan());
    }

    private RequestPostProcessor as(AuthPrincipal p) {
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role()))));
    }

    private long deckIn(AuthPrincipal owner, Visibility visibility) {
        Course c = courseRepository.save(new Course(owner.id(), "Course", visibility));
        return deckRepository.save(new Deck(c.getId(), "Deck")).getId();
    }

    private String body(long deckId, int total, int correct) {
        return "{\"deckId\":" + deckId + ",\"totalCards\":" + total + ",\"correctCount\":" + correct + "}";
    }

    // 4.1
    @Test
    void recordThenList() throws Exception {
        AuthPrincipal user = newUser();
        long deck = deckIn(user, Visibility.PRIVATE);

        mockMvc.perform(post("/study-sessions").with(as(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(deck, 10, 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deckId").value(deck))
                .andExpect(jsonPath("$.deckTitle").value("Deck"))
                .andExpect(jsonPath("$.correctCount").value(8));

        mockMvc.perform(get("/study-sessions").with(as(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].deckId").value(deck))
                .andExpect(jsonPath("$.content[0].deckTitle").value("Deck"));
    }

    // 4.2
    @Test
    void readability_publicOk_privateNotOwned404() throws Exception {
        AuthPrincipal owner = newUser();
        AuthPrincipal other = newUser();

        long publicDeck = deckIn(owner, Visibility.PUBLIC);
        mockMvc.perform(post("/study-sessions").with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(publicDeck, 5, 5)))
                .andExpect(status().isCreated());

        long privateDeck = deckIn(owner, Visibility.PRIVATE);
        mockMvc.perform(post("/study-sessions").with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(privateDeck, 5, 5)))
                .andExpect(status().isNotFound());
    }

    // 4.3
    @Test
    void sessionsArePrivateToTheUser() throws Exception {
        AuthPrincipal a = newUser();
        AuthPrincipal b = newUser();
        long deck = deckIn(a, Visibility.PRIVATE);

        mockMvc.perform(post("/study-sessions").with(as(a)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(deck, 3, 2)))
                .andExpect(status().isCreated());

        // b sees none of a's sessions
        mockMvc.perform(get("/study-sessions").with(as(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // 4.4
    @Test
    void validation_badCounts_400() throws Exception {
        AuthPrincipal user = newUser();
        long deck = deckIn(user, Visibility.PRIVATE);

        // correct > total
        mockMvc.perform(post("/study-sessions").with(as(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(deck, 5, 10)))
                .andExpect(status().isBadRequest());

        // negative total (Bean Validation @Min(0))
        mockMvc.perform(post("/study-sessions").with(as(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(deck, -1, 0)))
                .andExpect(status().isBadRequest());
    }
}
