package com.example.userservice;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory H2 database with sample data on every startup.
 * Remove this class (or make it @Profile("local")) in production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder().name("Alice Smith").email("alice@example.com").age(30).build());
            userRepository.save(User.builder().name("Bob Jones").email("bob@example.com").age(25).build());
            userRepository.save(User.builder().name("Carol White").email("carol@example.com").age(35).build());
            log.info("Seeded {} demo users", userRepository.count());
        }
    }
}
