package com.example.backendtest.user;

import java.time.Instant;

/** v1 view of a user. Never carries the password hash. */
public record UserResponse(Long id, String name, String email, Integer age, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAge(), user.getRole(),
                user.getCreatedAt());
    }
}
