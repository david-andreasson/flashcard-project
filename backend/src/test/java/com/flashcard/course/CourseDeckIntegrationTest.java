package com.flashcard.course;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.admin-email=seedadmin@example.com")
class CourseDeckIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private PublicCourseSeeder seeder;

    private AuthPrincipal newUser(Role role) {
        User u = userRepository.save(
                new User("u-" + UUID.randomUUID() + "@x.com", "hash", role, Plan.FREE));
        return new AuthPrincipal(u.getId(), u.getEmail(), role, u.getPlan());
    }

    private RequestPostProcessor as(AuthPrincipal p) {
        var token = new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
        return authentication(token);
    }

    private long createCourse(AuthPrincipal p, String title) throws Exception {
        var res = mockMvc.perform(post("/courses").with(as(p)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createDeck(AuthPrincipal p, long courseId, String title) throws Exception {
        var res = mockMvc.perform(post("/courses/" + courseId + "/decks").with(as(p)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private void makePublic(AuthPrincipal admin, long courseId) throws Exception {
        mockMvc.perform(put("/courses/" + courseId).with(as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk());
    }

    // 8.1 — ownership
    @Test
    void owner_canCrud_nonOwner_gets404() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        long courseId = createCourse(owner, "Mine");

        mockMvc.perform(get("/courses/" + courseId).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mine"));

        // non-owner cannot read/update/delete a private course → 404
        mockMvc.perform(get("/courses/" + courseId).with(as(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/courses/" + courseId).with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Hacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/courses/" + courseId).with(as(other)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // 8.2 — visibility
    @Test
    void publicCourse_readableByAnyone_butOnlyAdminCanPublish() throws Exception {
        AuthPrincipal admin = newUser(Role.ADMIN);
        AuthPrincipal user = newUser(Role.USER);

        long adminCourse = createCourse(admin, "Official");
        makePublic(admin, adminCourse);

        // any user can read the public course
        mockMvc.perform(get("/courses/" + adminCourse).with(as(user)))
                .andExpect(status().isOk());

        // a non-admin cannot make their own course public
        long userCourse = createCourse(user, "My private");
        mockMvc.perform(put("/courses/" + userCourse).with(as(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isForbidden());

        // admin can modify any course
        mockMvc.perform(put("/courses/" + userCourse).with(as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Renamed by admin\"}"))
                .andExpect(status().isOk());
    }

    // 8.3 — cascade
    @Test
    void deletingCourse_removesItsDecks() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        long courseId = createCourse(owner, "Course");
        long deckId = createDeck(owner, courseId, "Deck");

        mockMvc.perform(delete("/courses/" + courseId).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        // the course is gone, and listing its decks now 404s (course not found)
        mockMvc.perform(get("/courses/" + courseId + "/decks").with(as(owner)))
                .andExpect(status().isNotFound());
    }

    // 8.4 — deck nesting
    @Test
    void decks_underOwnedCourse_work_underOthers_404() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        AuthPrincipal other = newUser(Role.USER);
        long courseId = createCourse(owner, "Course");
        long deckId = createDeck(owner, courseId, "Deck 1");

        mockMvc.perform(get("/courses/" + courseId + "/decks/" + deckId).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Deck 1"));

        // other user cannot create or read decks under a private course they don't own
        mockMvc.perform(post("/courses/" + courseId + "/decks").with(as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sneaky\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/courses/" + courseId + "/decks/" + deckId).with(as(other)))
                .andExpect(status().isNotFound());
    }

    // 8.5 — pagination
    @Test
    void list_returnsPagedResponse_andCapsSize() throws Exception {
        AuthPrincipal owner = newUser(Role.USER);
        createCourse(owner, "A");
        createCourse(owner, "B");

        mockMvc.perform(get("/courses?scope=mine&page=0&size=1").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        // oversized size is capped at MAX_SIZE (100)
        mockMvc.perform(get("/courses?scope=mine&size=9999").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    // 8.6 — seeding
    @Test
    void seeder_createsPublicCourses_idempotently() {
        // The seeder targets app.admin-email, overridden to seedadmin@example.com for this test.
        User configuredAdmin = userRepository.save(
                new User("seedadmin@example.com", "h", Role.ADMIN, Plan.PREMIUM));

        seeder.run(null);
        long after1 = courseRepository.findByOwnerId(configuredAdmin.getId(),
                org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();
        assertThat(after1).isEqualTo(PublicCourseSeeder.PUBLIC_COURSE_TITLES.size());

        seeder.run(null); // second run — no duplicates
        long after2 = courseRepository.findByOwnerId(configuredAdmin.getId(),
                org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();
        assertThat(after2).isEqualTo(PublicCourseSeeder.PUBLIC_COURSE_TITLES.size());
    }
}
