
package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class InitialAdminService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public InitialAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void createInitialAdmin(
            String username,
            String email,
            String password) {

        // Check whether an ADMIN already exists
        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new IllegalStateException(
                    "Initial Admin has already been created");
        }

        // Validate username
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required");
        }

        // Validate email
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required");
        }

        // Validate password
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters");
        }

        // Check duplicate username
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException(
                    "Username already exists");
        }

        // Check duplicate email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "Email already exists");
        }

        // Create Admin user
        User admin = new User();

        admin.setUserId(null);
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);

        LocalDateTime now = LocalDateTime.now();

        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        // Save Admin in MySQL
        userRepository.save(admin);
    }
}
