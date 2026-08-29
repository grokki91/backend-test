package com.example.backendtest;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping
    public PageResponse list(@RequestParam(required = false) String name,
                             @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                             Pageable pageable) {
        Page<User> page = (name == null || name.isBlank())
                ? users.findAll(pageable)
                : users.findByNameContainingIgnoreCase(name, pageable);
        return new PageResponse(page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return UserResponse.from(find(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already in use: " + request.email());
        }
        User user = users.save(new User(request.name(), request.email(), encoder.encode(request.password()),
                request.age(), User.ROLE_USER));
        return ResponseEntity.created(URI.create("/api/users/" + user.getId())).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = find(id);
        if (!user.getEmail().equalsIgnoreCase(request.email()) && users.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already in use: " + request.email());
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        return UserResponse.from(users.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestAttribute(AuthInterceptor.ROLE) String role) {
        if (!User.ROLE_ADMIN.equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only ADMIN can delete users");
        }
        users.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private User find(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    public record PageResponse(List<UserResponse> items, int page, int size, long total, int totalPages) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @Min(0) @Max(150) Integer age) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(min = 2, max = 50) String name,
            @NotBlank @Email String email,
            @Min(0) @Max(150) Integer age) {
    }
}
