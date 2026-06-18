package com.flashcard.user;

import com.flashcard.config.AppProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    /**
     * Registers a new user. The email must be unique. The password is BCrypt-hashed.
     * A user registering with the configured admin email is granted ADMIN; everyone
     * else defaults to USER and plan FREE.
     *
     * @throws EmailAlreadyUsedException if the email is already taken
     */
    @Transactional
    public User register(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }
        Role role = normalizedEmail.equalsIgnoreCase(appProperties.adminEmail())
                ? Role.ADMIN
                : Role.USER;
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                role,
                Plan.FREE
        );
        return userRepository.save(user);
    }
}
