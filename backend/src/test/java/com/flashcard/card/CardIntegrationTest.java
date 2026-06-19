package com.flashcard.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcard.auth.AuthPrincipal;
import com.flashcard.course.Course;
import com.flashcard.course.CourseRepository;
import com.flashcard.course.PublicCourseSeeder;
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
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.admin-email=cardadmin@example.com")
class CardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private PublicCourseSeeder publicCourseSeeder;
    @Autowired private ContentSeeder contentSeeder;

    private AuthPrincipal newUser(Role role) {
        User u = userRepository.save(new User("u-" + UUID.randomUUID() + "@x.com", "h", role, Plan.FREE));
        return new AuthPrincipal(u.getId(), u.getEmail(), role, u.getPlan());
    }

    private RequestPostProcessor as(AuthPrincipal p) {
        return authentication(new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role()))));
    }

    private long course(AuthPrincipal owner, Visibility v) {
        return courseRepository.save(new Course(owner.id(), "Course", v)).getId();
    }

    private long deck(long courseId) {
        return deckRepository.save(new Deck(courseId, "Deck")).getId();
    }

    private String cardsUrl(long courseId, long deckId) {
        return "/courses/" + courseId + "/decks/" + deckId + "/cards";
    }

    // 5.1 + 5.6
    @Test
    void cardCrud_underOwnedCourse() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);

        mockMvc.perform(post(cardsUrl(c, d)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"hello\",\"back\":\"hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.front").value("hello"));

        mockMvc.perform(post(cardsUrl(c, d)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"bye\",\"back\":\"adios\"}"))
                .andExpect(status().isCreated());

        // list ordered by createdAt ASC
        mockMvc.perform(get(cardsUrl(c, d)).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].front").value("hello"))
                .andExpect(jsonPath("$.content[1].front").value("bye"));
    }

    // 5.2
    @Test
    void visibility_publicReadableByAnyone_privateHiddenFromNonOwner() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);

        long pub = course(owner, Visibility.PUBLIC);
        long pubDeck = deck(pub);
        cardRepository.save(new Card(pubDeck, "q", "a", null));
        mockMvc.perform(get(cardsUrl(pub, pubDeck)).with(as(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        long priv = course(owner, Visibility.PRIVATE);
        long privDeck = deck(priv);
        mockMvc.perform(get(cardsUrl(priv, privDeck)).with(as(other)))
                .andExpect(status().isNotFound());
    }

    // 5.3
    @Test
    void writeProtection_nonOwner404_adminCanWrite() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        AuthPrincipal admin = newUser(Role.ADMIN);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);

        mockMvc.perform(post(cardsUrl(c, d)).with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"x\",\"back\":\"y\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(cardsUrl(c, d)).with(as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"x\",\"back\":\"y\"}"))
                .andExpect(status().isCreated());
    }

    // 5.4
    @Test
    void validation_blankFront_returns400() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        mockMvc.perform(post(cardsUrl(c, d)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"\",\"back\":\"y\"}"))
                .andExpect(status().isBadRequest());
    }

    // 5.5 — user-visible cascade (physical FK cascade is DB-level, verified live on PostgreSQL)
    @Test
    void deletingDeck_makesItsCardsInaccessible() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        cardRepository.save(new Card(d, "q", "a", null));

        mockMvc.perform(delete("/courses/" + c + "/decks/" + d).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(cardsUrl(c, d)).with(as(owner)))
                .andExpect(status().isNotFound());
    }

    // 5.7
    @Test
    void seeder_populatesPublicCourses_idempotent() throws Exception {
        userRepository.save(new User("cardadmin@example.com", "h", Role.ADMIN, Plan.PREMIUM));
        publicCourseSeeder.run(null);   // course shells
        contentSeeder.run(null);        // decks + cards from JSON

        Course spanish = courseRepository
                .findByOwnerIdAndTitle(userRepository.findByEmail("cardadmin@example.com").orElseThrow().getId(),
                        "Spanish Vocabulary")
                .orElseThrow();
        Deck deck = deckRepository.findByCourseIdAndTitle(spanish.getId(), "Greetings & Basics").orElseThrow();
        long cardsAfter1 = cardRepository.findByDeckId(deck.getId(), PageRequest.of(0, 100)).getTotalElements();
        assertThat(cardsAfter1).isEqualTo(10);

        contentSeeder.run(null); // second run — no duplicates
        long cardsAfter2 = cardRepository.findByDeckId(deck.getId(), PageRequest.of(0, 100)).getTotalElements();
        assertThat(cardsAfter2).isEqualTo(10);
    }
}
