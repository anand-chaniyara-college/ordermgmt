package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // REGISTER API
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestBody RegistrationRequestDTO request) {
        System.out.println(">>> [AuthController] Received Register request: " + request.getEmail());

        String result = authService.registerUser(request);

        if ("Email already exists".equals(result) || "Role not found".equals(result)) {
            return ResponseEntity.badRequest().body(new RegistrationResponseDTO(result));
        }
        return ResponseEntity.ok(new RegistrationResponseDTO(result));
    }

    // LOGIN API
    // 1. Client sends Email + Password
    // 2. We verify it.
    // 3. We return a Token if valid.
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        System.out.println(">>> [AuthController] Received Login request: " + request.getEmail());

        LoginResponseDTO response = authService.loginUser(request);

        // Standard HTTP Check
        if ("Login successful".equals(response.getMessage())) {
            // 200 OK
            return ResponseEntity.ok(response);
        } else {
            // 401 Unauthorized (Wrong password / User not found)
            return ResponseEntity.status(401).body(response);
        }
    }
}
