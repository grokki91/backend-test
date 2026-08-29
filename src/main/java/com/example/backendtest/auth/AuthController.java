package com.example.backendtest.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendtest.common.ApiException;
import com.example.backendtest.user.User;
import com.example.backendtest.user.UserRepository;
import com.example.backendtest.user.UserResponse;
import com.example.backendtest.user.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository users;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final String machineClientId;
    private final String machineClientSecret;

    public AuthController(UserRepository users, UserService userService, PasswordEncoder encoder,
                          JwtService jwtService,
                          @Value("${app.oauth.client-id}") String machineClientId,
                          @Value("${app.oauth.client-secret}") String machineClientSecret) {
        this.users = users;
        this.userService = userService;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.machineClientId = machineClientId;
        this.machineClientSecret = machineClientSecret;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.create(request.name(), request.email(), request.password(), request.age(),
                User.ROLE_USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .filter(candidate -> encoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        long lifetime = request.expiresInSeconds() == null ? jwtService.ttlSeconds() : request.expiresInSeconds();
        return new TokenResponse(jwtService.generate(user.getId(), user.getEmail(), user.getRole(), lifetime),
                "Bearer", lifetime, user.getRole());
    }

    /**
     * OAuth 2.0 client_credentials, form-encoded like the real thing — the grant a
     * service-to-service caller would use.
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse token(@RequestParam("grant_type") String grantType,
                               @RequestParam("client_id") String clientId,
                               @RequestParam("client_secret") String clientSecret) {
        if (!"client_credentials".equals(grantType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported grant_type: " + grantType);
        }
        if (!machineClientId.equals(clientId) || !machineClientSecret.equals(clientSecret)) {
            throw ApiException.unauthorized("Invalid client credentials");
        }
        return new TokenResponse(jwtService.generate(0L, clientId, User.ROLE_ADMIN), "Bearer",
                jwtService.ttlSeconds(), User.ROLE_ADMIN);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return userService.getView(userId);
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @Min(0) @Max(150) Integer age) {
    }

    /** {@code expiresInSeconds} is a test hook: ask for a token that expires almost immediately. */
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @Min(1) @Max(86400) Long expiresInSeconds) {
    }

    public record TokenResponse(String token, String tokenType, long expiresIn, String role) {
    }
}
