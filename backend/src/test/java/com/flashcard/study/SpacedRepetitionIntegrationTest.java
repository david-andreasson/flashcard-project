package com.flashcard.study;

import com.flashcard.auth.AuthPrincipal;
import com.flashcard.card.Card;
import com.flashcard.card.CardRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpacedRepetitionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCardStateRepository stateRepository;

    private AuthPrincipal newUser(Role role) {
        User u = userRepository.save(new User("sr-" + UUID.randomUUID() + "@x.com", "h", role, Plan.FREE));
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

    private long card(long deckId, String front) {
        return cardRepository.save(new Card(deckId, front, "back", null)).getId();
    }

    private String reviewUrl(long c, long d, long cardId) {
        return "/courses/" + c + "/decks/" + d + "/cards/" + cardId + "/review";
    }

    private String dueUrl(long c, long d) {
        return "/courses/" + c + "/decks/" + d + "/due";
    }

    private void seedState(Long userId, Long cardId, Instant dueAt) {
        UserCardState s = new UserCardState(userId, cardId);
        s.setDueAt(dueAt);
        s.setLastReviewedAt(Instant.now());
        s.setIntervalDays(1);
        s.setRepetitions(1);
        s.setEaseFactor(2.5);
        stateRepository.save(s);
    }

    // 7.2 — first review lazily creates state and returns the schedule
    @Test
    void firstReview_createsState_andReturnsSchedule() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        long cardId = card(d, "q");

        mockMvc.perform(post(reviewUrl(c, d, cardId)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.repetitions").value(1));

        assertThat(stateRepository.findByUserIdAndCardId(owner.id(), cardId)).isPresent();
    }

    // 7.2 — review in an unreadable deck is 404 and writes no state
    @Test
    void review_unreadableDeck_404_noState() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        long cardId = card(d, "q");

        mockMvc.perform(post(reviewUrl(c, d, cardId)).with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"GOOD\"}"))
                .andExpect(status().isNotFound());
        assertThat(stateRepository.findByUserIdAndCardId(other.id(), cardId)).isEmpty();
    }

    // 7.2 — missing/invalid grade is 400
    @Test
    void review_invalidGrade_400() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        long cardId = card(d, "q");
        mockMvc.perform(post(reviewUrl(c, d, cardId)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // 7.2 — the same shared card schedules independently per user
    @Test
    void review_isPerUser() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        long c = course(owner, Visibility.PUBLIC);
        long d = deck(c);
        long cardId = card(d, "q");

        mockMvc.perform(post(reviewUrl(c, d, cardId)).with(as(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"EASY\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(reviewUrl(c, d, cardId)).with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"AGAIN\"}"))
                .andExpect(status().isOk());

        assertThat(stateRepository.findByUserIdAndCardId(owner.id(), cardId).orElseThrow().getRepetitions()).isEqualTo(1);
        assertThat(stateRepository.findByUserIdAndCardId(other.id(), cardId).orElseThrow().getRepetitions()).isZero();
    }

    // 7.3 — due queue returns new + overdue, excludes future
    @Test
    void dueQueue_returnsNewAndOverdue_excludesFuture() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        card(d, "new");                                            // new (no state) → due
        long overdue = card(d, "overdue");
        long future = card(d, "future");
        seedState(owner.id(), overdue, Instant.now().minus(1, ChronoUnit.DAYS)); // due
        seedState(owner.id(), future, Instant.now().plus(10, ChronoUnit.DAYS));  // not due

        mockMvc.perform(get(dueUrl(c, d)).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // 7.3 — due queue for an unreadable deck is 404
    @Test
    void dueQueue_unreadableDeck_404() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        long c = course(owner, Visibility.PRIVATE);
        long d = deck(c);
        mockMvc.perform(get(dueUrl(c, d)).with(as(other)))
                .andExpect(status().isNotFound());
    }

    // 7.4 — progress counts reflect only the caller's state
    @Test
    void progress_countsForUser() throws Exception {
        AuthPrincipal u = newUser(Role.USER);
        long c = course(u, Visibility.PRIVATE);
        long d = deck(c);
        seedState(u.id(), card(d, "a"), Instant.now().minus(1, ChronoUnit.DAYS)); // due now
        seedState(u.id(), card(d, "b"), Instant.now().plus(5, ChronoUnit.DAYS));  // not due

        mockMvc.perform(get("/study/progress").with(as(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inReview").value(2))
                .andExpect(jsonPath("$.dueNow").value(1))
                .andExpect(jsonPath("$.reviewedToday").value(2));
    }
}
