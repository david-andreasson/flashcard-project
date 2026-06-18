package com.flashcard.course;

import com.flashcard.config.AppProperties;
import com.flashcard.user.User;
import com.flashcard.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ensures a set of official PUBLIC courses owned by the configured admin exists. Runs on
 * startup and is idempotent — it only inserts courses that are missing, so restarts and a
 * not-yet-registered admin are both handled gracefully.
 */
@Component
public class PublicCourseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicCourseSeeder.class);

    /** Official public course titles. Cards/decks are added later (change 04). */
    static final List<String> PUBLIC_COURSE_TITLES = List.of(
            "Spanish Vocabulary",
            "French Vocabulary",
            "World Capitals",
            "Human Anatomy",
            "Java Fundamentals",
            "SQL Basics",
            "Cell Biology",
            "Art History",
            "Music Theory",
            "Chemistry Elements"
    );

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AppProperties appProperties;

    public PublicCourseSeeder(UserRepository userRepository,
                              CourseRepository courseRepository,
                              AppProperties appProperties) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminEmail = appProperties.adminEmail();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            log.info("Public course seeding skipped: admin user '{}' not registered yet", adminEmail);
            return;
        }
        int created = 0;
        for (String title : PUBLIC_COURSE_TITLES) {
            if (!courseRepository.existsByOwnerIdAndTitle(admin.getId(), title)) {
                courseRepository.save(new Course(admin.getId(), title, Visibility.PUBLIC));
                created++;
            }
        }
        if (created > 0) {
            log.info("Seeded {} public course(s) owned by admin '{}'", created, adminEmail);
        }
    }
}
