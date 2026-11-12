package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder =
                new BCryptPasswordEncoder();
    }

    public User registerUser(User user) {

        if (user.getUsername() == null ||
                user.getUsername().isBlank()) {

            throw new RuntimeException(
                    "Username is required");
        }

        if (user.getEmail() == null ||
                user.getEmail().isBlank()) {

            throw new RuntimeException(
                    "Email is required");
        }

        if (user.getPassword() == null ||
                user.getPassword().isBlank()) {

            throw new RuntimeException(
                    "Password is required");
        }

        if (user.getPassword().length() < 8) {

            throw new RuntimeException(
                    "Password must contain at least 8 characters");
        }

        if (userRepository
                .findByUsername(user.getUsername())
                .isPresent()) {

            throw new RuntimeException(
                    "Username is already taken");
        }

        if (userRepository
                .findByEmail(user.getEmail())
                .isPresent()) {

            throw new RuntimeException(
                    "Email is already registered");
        }

        user.setUserId(null);

        user.setRole(Role.CUSTOMER);

        // New customers are active by default.
        user.setActive(true);

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()));

        LocalDateTime now =
                LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }
}