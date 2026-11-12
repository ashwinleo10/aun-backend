
package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.JWTToken;
import com.example.demo.entity.User;
import com.example.demo.repository.JWTTokenRepository;
import com.example.demo.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final long JWT_EXPIRATION = 3600000; // 1 hour

    private final Key SIGNING_KEY;
    private final UserRepository userRepository;
    private final JWTTokenRepository jwtTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            JWTTokenRepository jwtTokenRepository,
            @Value("${jwt.secret}") String jwtSecret) {

        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 64 bytes long for HS512."
            );
        }

        this.SIGNING_KEY = Keys.hmacShaKeyFor(
            jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // 1. Authenticate user using username and password
    public User authenticate(String username, String password) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new RuntimeException("Invalid username or password")
            );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }

    // 2. Generate or reuse JWT token
    public String generateToken(User user) {

        LocalDateTime now = LocalDateTime.now();

        JWTToken existingToken =
            jwtTokenRepository.findByUserId(user.getUserId());

        if (existingToken != null
                && now.isBefore(existingToken.getExpiresAt())
                && validateToken(existingToken.getToken())) {

            return existingToken.getToken();
        }

        if (existingToken != null) {
            jwtTokenRepository.delete(existingToken);
        }

        String token = generateNewToken(user);
        saveToken(user, token);

        return token;
    }

    // 3. Generate a new JWT token
    public String generateNewToken(User user) {

        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("role", user.getRole().name())
            .setIssuedAt(new Date())
            .setExpiration(
                new Date(System.currentTimeMillis() + JWT_EXPIRATION)
            )
            .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
            .compact();
    }

    // 4. Save JWT token in database
    public void saveToken(User user, String token) {

        JWTToken jwtToken = new JWTToken(
            user,
            token,
            LocalDateTime.now().plusHours(1)
        );

        jwtTokenRepository.save(jwtToken);
    }

    // 5. Validate JWT signature, expiry and database record
    public boolean validateToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // Verify JWT signature and expiration
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

            String username = claims.getSubject();

            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            // Check whether the user exists
            User user = userRepository.findByUsername(username)
                .orElse(null);

            if (user == null) {
                return false;
            }

            // Check whether this exact token is stored in the database
            JWTToken storedToken =
                jwtTokenRepository.findByUserId(user.getUserId());

            if (storedToken == null) {
                return false;
            }

            if (!token.equals(storedToken.getToken())) {
                return false;
            }

            // Check database expiry as well
            return LocalDateTime.now().isBefore(
                storedToken.getExpiresAt()
            );

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 6. Extract username from JWT token
    public String extractUsername(String token) {

        Claims claims = Jwts.parserBuilder()
            .setSigningKey(SIGNING_KEY)
            .build()
            .parseClaimsJws(token)
            .getBody();

        return claims.getSubject();
    }
}
