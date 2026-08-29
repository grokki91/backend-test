package com.example.backendtest;

import java.time.Instant;

/** Public view of a user — never carries the password hash. */
public record UserResponse(Long id, String name, String email, Integer age, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAge(), user.getRole(),
                user.getCreatedAt());
    }
}
