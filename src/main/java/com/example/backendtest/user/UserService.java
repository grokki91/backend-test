package com.example.backendtest.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backendtest.common.ApiException;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public Page<User> list(String name, Pageable pageable) {
        return (name == null || name.isBlank())
                ? users.findAll(pageable)
                : users.findByNameContainingIgnoreCase(name, pageable);
    }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }

    /**
     * The read path REST uses. Cache-aside via Redis: the first read fills the entry and
     * the writes below evict it. Only the public view is cached, so no password hash
     * ever reaches Redis.
     */
    @Cacheable(cacheNames = "users", key = "#id")
    public UserResponse getView(Long id) {
        return UserResponse.from(get(id));
    }

    @Transactional
    public User create(String name, String email, String rawPassword, Integer age, String role) {
        requireEmailFree(email, null);
        return users.save(new User(name, email, encoder.encode(rawPassword), age, role));
    }

    @Transactional
    @CacheEvict(cacheNames = "users", key = "#id")
    public User replace(Long id, UserRequests.Replace request) {
        User user = get(id);
        requireEmailFree(request.email(), user);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        return users.save(user);
    }

    @Transactional
    @CacheEvict(cacheNames = "users", key = "#id")
    public User patch(Long id, UserRequests.Patch request) {
        User user = get(id);
        if (request.email() != null) {
            requireEmailFree(request.email(), user);
            user.setEmail(request.email());
        }
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        return users.save(user);
    }

    @Transactional
    @CacheEvict(cacheNames = "users", key = "#id")
    public void delete(Long id) {
        users.delete(get(id));
    }

    public boolean emailTaken(String email) {
        return users.existsByEmailIgnoreCase(email);
    }

    private void requireEmailFree(String email, User current) {
        boolean unchanged = current != null && current.getEmail().equalsIgnoreCase(email);
        if (!unchanged && users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email already in use: " + email);
        }
    }
}
