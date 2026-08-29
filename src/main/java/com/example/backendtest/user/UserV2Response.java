package com.example.backendtest.user;

import java.time.Instant;

/**
 * v2 view: {@code name} became {@code fullName}, {@code email} became {@code contactEmail}
 * and {@code active} was added. Deliberately breaking, so v1/v2 backward compatibility
 * is something you can actually assert.
 */
public record UserV2Response(Long id, String fullName, String contactEmail, Integer age, String role, boolean active,
                             Instant createdAt) {

    public static UserV2Response from(User user) {
        return from(UserResponse.from(user));
    }

    public static UserV2Response from(UserResponse user) {
        return new UserV2Response(user.id(), user.name(), user.email(), user.age(), user.role(), true,
                user.createdAt());
    }
}
