
package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.InitialAdminRequest;
import com.example.demo.service.InitialAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/setup")
public class InitialAdminController {

    private final InitialAdminService initialAdminService;

    @Value("${app.admin.setup-secret:}")
    private String setupSecret;

    public InitialAdminController(
            InitialAdminService initialAdminService) {
        this.initialAdminService = initialAdminService;
    }

    @PostMapping("/initial-admin")
    public ResponseEntity<Map<String, String>> createInitialAdmin(
            @RequestHeader(value = "X-SETUP-SECRET", required = false)
            String providedSecret,
            @Valid @RequestBody InitialAdminRequest request) {

        if (setupSecret == null || setupSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message", "Initial Admin setup is not configured"));
        }

        if (providedSecret == null ||
                !MessageDigest.isEqual(
                        setupSecret.getBytes(StandardCharsets.UTF_8),
                        providedSecret.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message", "Invalid setup secret"));
        }

        initialAdminService.createInitialAdmin(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Initial Admin created successfully"
                ));
    }
}
