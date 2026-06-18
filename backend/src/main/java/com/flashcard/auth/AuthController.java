package com.flashcard.auth;

import com.flashcard.auth.dto.LoginRequest;
import com.flashcard.auth.dto.RegisterRequest;
import com.flashcard.auth.dto.UserResponse;
import com.flashcard.user.User;
import com.flashcard.user.UserRepository;
import com.flashcard.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;

    public AuthController(UserService userService,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService,
                         RefreshTokenService refreshTokenService,
                         CookieService cookieService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password());
        String refreshToken = refreshTokenService.issue(user);
        return authResponse(user, refreshToken, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String refreshToken = refreshTokenService.issue(user);
        return authResponse(user, refreshToken, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(
            @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }
        RefreshTokenService.Issued issued = refreshTokenService.rotate(refreshToken);
        return authResponse(issued.user(), issued.rawToken(), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) String refreshToken) {
        refreshTokenService.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return UserResponse.from(principal);
    }

    /** Issues a fresh access token, sets both auth cookies, and returns the user body. */
    private ResponseEntity<UserResponse> authResponse(User user, String rawRefreshToken, HttpStatus status) {
        String accessToken = jwtService.generateAccessToken(user);
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieService.accessCookie(accessToken).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.refreshCookie(rawRefreshToken).toString())
                .body(UserResponse.from(user));
    }
}
