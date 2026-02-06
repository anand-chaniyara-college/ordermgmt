package com.example.ordermgmt.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // Minimum 256-bit key for HMAC-SHA256 (32 characters safe choice)
    // In production, this should be in application.properties
    // ERROR FIX: The previous key was too short (31 chars). It checks size in BITS.
    // "LearningSpringbootIsIntresting!" is 248 bits. We need >= 256 bits.
    // I added "123" at the end to make it long enough.
    private static final String SECRET_STRING = "LearningSpringbootIsIntresting!123";
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    }

    // Generate Token
    // Creates a long string "eyJ..." that contains user's info encrypted/signed
    public String generateToken(String email, String role) {
        // 1 hour expiry (Security best practice: don't let tokens live forever)
        long expiryMillis = 1000 * 60 * 60;

        return Jwts.builder()
                .subject(email) // Who is this token for?
                .claim("role", role) // What allows do they have?
                .issuedAt(new Date()) // When was it created?
                .expiration(new Date(System.currentTimeMillis() + expiryMillis)) // When does it die?
                .signWith(key) // SIGN it with our secret key
                .compact(); // Compress into a string
    }
}
