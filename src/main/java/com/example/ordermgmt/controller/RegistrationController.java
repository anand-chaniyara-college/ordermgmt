package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================================
 * USER REGISTRATION LIFECYCLE (How it works):
 * ===================================================================================
 * 1. CLIENT (Postman/Browser) sends a POST request with JSON data:
 * { "email": "...", "password": "...", "roleName": "..." }
 * 
 * 2. CONTROLLER (This File) receives the request at "/api/auth/register".
 * - It converts the JSON into a Java Object (RegistrationRequestDTO).
 * - It calls the Service to handle the logic.
 * 
 * 3. SERVICE (RegistrationServiceImpl) processes the logic:
 * - Checks if email exists.
 * - Checks if role exists.
 * - Encrypts password.
 * - Saves to Database via Repository.
 * 
 * 4. REPOSITORY (AppUserRepository) talks to the Database (MySQL/Postgres)
 * - Runs the actual INSERT SQL command.
 * 
 * 5. CONTROLLER returns a response back to the Client:
 * - 200 OK if successful.
 * - 400 BAD REQUEST if validation failed.
 * ===================================================================================
 */

// @RestController: Tells Spring this class handles Web API requests and returns
// JSON.
@RestController
// @RequestMapping: The base URL for all methods in this class.
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    // Constructor Injection: Spring provides the service instance.
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // @PostMapping: Handles HTTP POST requests to "/register"
    // @RequestBody: Takes the JSON content from the request and putting it into
    // 'request' object
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestBody RegistrationRequestDTO request) {
        // Log entry into Controller
        System.out
                .println(">>> [RegistrationController] Received registration request for email: " + request.getEmail());

        // Delegate the work to the Service layer
        String result = registrationService.registerUser(request);

        // Log result from Service
        System.out.println(">>> [RegistrationController] Service returned result: " + result);

        // Check the result string (Simple error handling logic)
        if ("Email already exists".equals(result) || "Role not found".equals(result)) {
            // Return 400 Bad Request error code
            return ResponseEntity.badRequest().body(new RegistrationResponseDTO(result));
        }

        // Return 200 OK success code
        return ResponseEntity.ok(new RegistrationResponseDTO(result));
    }
}
