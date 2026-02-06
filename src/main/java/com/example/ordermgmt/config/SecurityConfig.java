package com.example.ordermgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Bean for password encryption
    // BCrypt is a strong hashing function widely used for passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean to configure security rules
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // We disable CSRF (Cross-Site Request Forgery) because we are building a
                // stateless API
                // and usually clients (like Frontend or Postman) don't use session cookies in
                // the same way browsers do for forms.
                .csrf(csrf -> csrf.disable())

                // Define which endpoints are public and which need authentication
                .authorizeHttpRequests(auth -> auth
                        // Allow anyone to access the registration endpoint
                        // We use /api/auth/** to allow any path under that prefix (register, login,
                        // etc)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Require authentication for any other request
                        .anyRequest().authenticated());

        return http.build();
    }
}
