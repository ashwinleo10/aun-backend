package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LoginRequest;
import com.example.demo.entity.User;
import com.example.demo.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(
	    origins = {
	        "http://localhost:5173",
	        "https://aun-frontend.vercel.app"
	    },
	    allowCredentials = "true"
	)
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        try {

            User user = authService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            String token = authService.generateToken(user);

            ResponseCookie cookie = ResponseCookie
                .from("authToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
                .sameSite("None")
                .build();

            response.setHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
            );

            Map<String, Object> responseBody = new HashMap<>();

            responseBody.put(
                "message",
                "Login successful"
            );

            responseBody.put(
                "role",
                user.getRole().name()
            );

            responseBody.put(
                "username",
                user.getUsername()
            );

            return ResponseEntity.ok(responseBody);

        } catch (RuntimeException e) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    Map.of(
                        "error",
                        e.getMessage()
                    )
                );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie
            .from("authToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("None")
            .build();

        response.setHeader(
            HttpHeaders.SET_COOKIE,
            cookie.toString()
        );

        return ResponseEntity.ok(
            Map.of(
                "message",
                "Logout successful"
            )
        );
    }
}