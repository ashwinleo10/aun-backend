
package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
@CrossOrigin(
    origins = "http://localhost:5173",
    allowCredentials = "true"
)
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {

        try {
            User registeredUser = userService.registerUser(user);

            return ResponseEntity.ok(
                Map.of(
                    "message", "User registered successfully",
                    "username", registeredUser.getUsername(),
                    "email", registeredUser.getEmail()
                )
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        }
    }
}
