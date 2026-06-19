package com.flashcard.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcard.config.AppProperties;
import com.flashcard.course.Course;
import com.flashcard.course.CourseRepository;
import com.flashcard.deck.Deck;
import com.flashcard.deck.DeckRepository;
import com.flashcard.user.User;
import com.flashcard.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Fills the admin-owned PUBLIC courses with sample decks and cards described in
 * {@code seed/public-content.json}. Runs after {@link com.flashcard.course.PublicCourseSeeder}
 * (which creates the course shells) and is idempotent: a deck is created only if its course
 * lacks one with that title, and a card only if its deck lacks one with that front.
 */
@Component
@Order(2)
public class ContentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentSeeder.class);
    private static final String SEED_RESOURCE = "seed/public-content.json";

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final AppProperties appProperties;

    public ContentSeeder(ObjectMapper objectMapper,
                         UserRepository userRepository,
                         CourseRepository courseRepository,
                         DeckRepository deckRepository,
                         CardRepository cardRepository,
                         AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.appProperties = appProperties;
    }

    // JSON shape of seed/public-content.json
    public record SeedContent(List<SeedCourse> courses) {}
    public record SeedCourse(String title, List<SeedDeck> decks) {}
    public record SeedDeck(String title, List<SeedCard> cards) {}
    public record SeedCard(String front, String back, String notes) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        User admin = userRepository.findByEmail(appProperties.adminEmail()).orElse(null);
        if (admin == null) {
            log.info("Content seeding skipped: admin user not registered yet");
            return;
        }
        SeedContent seed = readSeed();
        int decksCreated = 0;
        int cardsCreated = 0;

        for (SeedCourse sc : seed.courses()) {
            Course course = courseRepository.findByOwnerIdAndTitle(admin.getId(), sc.title()).orElse(null);
            if (course == null) {
                continue; // course shell not present; PublicCourseSeeder owns course creation
            }
            for (SeedDeck sd : sc.decks()) {
                Deck deck = deckRepository.findByCourseIdAndTitle(course.getId(), sd.title())
                        .orElse(null);
                if (deck == null) {
                    deck = deckRepository.save(new Deck(course.getId(), sd.title()));
                    decksCreated++;
                }
                for (SeedCard card : sd.cards()) {
                    if (!cardRepository.existsByDeckIdAndFront(deck.getId(), card.front())) {
                        cardRepository.save(new Card(deck.getId(), card.front(), card.back(), card.notes()));
                        cardsCreated++;
                    }
                }
            }
        }
        if (decksCreated > 0 || cardsCreated > 0) {
            log.info("Content seeding: created {} deck(s) and {} card(s)", decksCreated, cardsCreated);
        }
    }

    private SeedContent readSeed() throws IOException {
        try (InputStream in = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, SeedContent.class);
        }
    }
}
