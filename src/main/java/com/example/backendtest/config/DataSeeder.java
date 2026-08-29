package com.example.backendtest.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backendtest.order.Order;
import com.example.backendtest.order.OrderRepository;
import com.example.backendtest.user.User;
import com.example.backendtest.user.UserRepository;

/** Known fixtures on an empty database, so the API has something to answer with. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository users, OrderRepository orders, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            User admin = users.save(new User("Admin", "admin@test.com", encoder.encode("admin123"), 30,
                    User.ROLE_ADMIN));
            User alice = users.save(new User("Alice", "alice@test.com", encoder.encode("alice123"), 25,
                    User.ROLE_USER));
            User bob = users.save(new User("Bob", "bob@test.com", encoder.encode("bob12345"), 41, User.ROLE_USER));
            users.save(new User("Carol", "carol@test.com", encoder.encode("carol123"), 33, User.ROLE_USER));

            orders.save(new Order(alice.getId(), new BigDecimal("199.99"), "EUR", "Alice first order"));
            orders.save(new Order(alice.getId(), new BigDecimal("49.50"), "EUR", "Alice second order"));
            orders.save(new Order(bob.getId(), new BigDecimal("1250.00"), "USD", "Bob big order"));
            orders.save(new Order(admin.getId(), new BigDecimal("10.00"), "GBP", "Admin test order"));
        };
    }
}
