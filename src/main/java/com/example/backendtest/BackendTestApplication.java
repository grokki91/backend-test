package com.example.backendtest;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendTestApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Seeds a known admin plus a few users so the API has data on first start. */
    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            users.saveAll(List.of(
                    new User("Admin", "admin@test.com", encoder.encode("admin123"), 30, User.ROLE_ADMIN),
                    new User("Alice", "alice@test.com", encoder.encode("alice123"), 25, User.ROLE_USER),
                    new User("Bob", "bob@test.com", encoder.encode("bob12345"), 41, User.ROLE_USER),
                    new User("Carol", "carol@test.com", encoder.encode("carol123"), 33, User.ROLE_USER)));
        };
    }
}
