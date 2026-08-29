package com.example.backendtest.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payloads for the user endpoints, kept together so the validation rules are one page. */
public final class UserRequests {

    private UserRequests() {
    }

    public record Create(
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @Min(0) @Max(150) Integer age) {
    }

    /** PUT: full replacement, every field required. */
    public record Replace(
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Email String email,
            @Min(0) @Max(150) Integer age) {
    }

    /** PATCH: partial update, a null field means "leave as is". */
    public record Patch(
            @Size(min = 2, max = 50) String name,
            @Email String email,
            @Min(0) @Max(150) Integer age) {
    }
}
