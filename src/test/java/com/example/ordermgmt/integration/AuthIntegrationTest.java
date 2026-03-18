package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Auth & Provisioning Integration Test Suite
 * Covers the end-to-end flow from Super Admin login to Customer registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(1)
public class AuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Static variables to share state across @Order-ed tests
        private static String superAdminAccessToken;
        private static String superAdminRefreshToken;
        private static UUID enterpriseOrgId;
        private static String orgAdminAccessToken;
        private static String orgAdminRefreshToken;

        @Test
        @Order(1)
        @DisplayName("1.1. Login SUPER_ADMIN - Negative Cases")
        void loginSuperAdminNegative() throws Exception {
                // Wrong Password
                LoginRequestDTO wrongPass = new LoginRequestDTO("systesting", "superadmin@superemail.com", "wrong");
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongPass)))
                                .andExpect(status().isUnauthorized());

                // Wrong Subdomain
                LoginRequestDTO wrongSub = new LoginRequestDTO("unknown-org", "superadmin@superemail.com",
                                "superadminpassword");
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongSub)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @Order(2)
        @DisplayName("1.2. Login SUPER_ADMIN - Success")
        void loginSuperAdminSuccess() throws Exception {
                LoginRequestDTO loginRequest = new LoginRequestDTO(
                                "systesting",
                                "superadmin@superemail.com",
                                "superadminpassword");

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", notNullValue()))
                                .andExpect(jsonPath("$.role", is("SUPER_ADMIN")))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                LoginResponseDTO response = objectMapper.readValue(content, LoginResponseDTO.class);
                superAdminAccessToken = response.getAccessToken();
                superAdminRefreshToken = response.getRefreshToken();

                System.out.println("SUPER_ADMIN Access Token captured.");
        }

        @Test
        @Order(3)
        @DisplayName("2.1. Create Organization - Unauthorized Access")
        void createOrganizationUnauthorized() throws Exception {
                CreateOrganizationRequestDTO createOrgRequest = new CreateOrganizationRequestDTO("Ghost Org", "ghost",
                                "Desc");

                // No Token
                mockMvc.perform(post("/api/super-admin/organizations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrgRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @Order(4)
        @DisplayName("2.2. Create Organization - Success")
        void createOrganizationSuccess() throws Exception {
                CreateOrganizationRequestDTO createOrgRequest = new CreateOrganizationRequestDTO(
                                "Enterprise Corp",
                                "enterprise",
                                "Enterprise level business organization");

                MvcResult result = mockMvc.perform(post("/api/super-admin/organizations")
                                .header("Authorization", "Bearer " + superAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrgRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name", is("Enterprise Corp")))
                                .andExpect(jsonPath("$.subdomain", is("enterprise")))
                                .andExpect(jsonPath("$.isActive", is(true)))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                OrganizationResponseDTO response = objectMapper.readValue(content, OrganizationResponseDTO.class);
                enterpriseOrgId = response.getOrgId();

                System.out.println("Organization 'enterprise' created with ID: " + enterpriseOrgId);
        }

        @Test
        @Order(5)
        @DisplayName("3. Create ORG_ADMIN for 'enterprise'")
        void createOrgAdmin() throws Exception {
                CreateOrgAdminRequestDTO createOrgAdminRequest = new CreateOrgAdminRequestDTO(
                                "vpsciencememories@gmail.com",
                                "orgadminpassword",
                                enterpriseOrgId);

                mockMvc.perform(post("/api/super-admin/org-admins")
                                .header("Authorization", "Bearer " + superAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrgAdminRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email", is("vpsciencememories@gmail.com")))
                                .andExpect(jsonPath("$.role", is("ORG_ADMIN")))
                                .andExpect(jsonPath("$.orgId", is(enterpriseOrgId.toString())));

                System.out.println("ORG_ADMIN created for 'enterprise'.");
        }

        @Test
        @Order(6)
        @DisplayName("4. Logout SUPER_ADMIN and confirm blacklist handling")
        void logoutSuperAdmin() throws Exception {
                RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO(superAdminRefreshToken);

                // First Logout - Should succeed
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + superAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", containsString("Logged out")));

                // Immediate subsequent call - Should fail (401 Unauthorized because accessToken
                // is blacklisted)
                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + superAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                                .andExpect(status().isUnauthorized());

                System.out.println("SUPER_ADMIN logged out and token blacklisted successfully.");
        }

        @Test
        @Order(7)
        @DisplayName("5. Login ORG_ADMIN and attempt Forbidden Access")
        void loginOrgAdminAndCheckForbidden() throws Exception {
                LoginRequestDTO loginRequest = new LoginRequestDTO(
                                "enterprise",
                                "vpsciencememories@gmail.com",
                                "orgadminpassword");

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken", notNullValue()))
                                .andExpect(jsonPath("$.role", is("ORG_ADMIN")))
                                .andReturn();

                String content = result.getResponse().getContentAsString();
                LoginResponseDTO response = objectMapper.readValue(content, LoginResponseDTO.class);
                orgAdminAccessToken = response.getAccessToken();
                orgAdminRefreshToken = response.getRefreshToken();

                // Attempt /api/super-admin/organizations -> should return 403 Forbidden
                mockMvc.perform(get("/api/super-admin/organizations")
                                .header("Authorization", "Bearer " + orgAdminAccessToken))
                                .andExpect(status().isForbidden());

                // Attempt to create another Org Admin (Only Super Admin can do this)
                CreateOrgAdminRequestDTO forbiddenAdminRequest = new CreateOrgAdminRequestDTO("hack@org.com", "pass123",
                                enterpriseOrgId);
                mockMvc.perform(post("/api/super-admin/org-admins")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(forbiddenAdminRequest)))
                                .andExpect(status().isForbidden());

                System.out.println("ORG_ADMIN logged in. Forbidden access (403) verified.");
        }

        @Test
        @Order(8)
        @DisplayName("6. ORG_ADMIN creates an ADMIN")
        void createAdmin() throws Exception {
                CreateAdminRequestDTO createAdminRequest = new CreateAdminRequestDTO(
                                "anandchaniyara007@gmail.com",
                                "adminpassword");

                mockMvc.perform(post("/api/org-admin/admins")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createAdminRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email", is("anandchaniyara007@gmail.com")))
                                .andExpect(jsonPath("$.role", is("ADMIN")))
                                .andExpect(jsonPath("$.orgId", is(enterpriseOrgId.toString())));

                System.out.println("ADMIN created by ORG_ADMIN.");
        }

        @Test
        @Order(9)
        @DisplayName("7. Public Registration for CUSTOMER")
        void registerCustomer() throws Exception {
                RegistrationRequestDTO registrationRequest = new RegistrationRequestDTO(
                                "anandchaniyara007storage@gmail.com",
                                "customerpassword",
                                "CUSTOMER",
                                "enterprise");

                // First registration - Should succeed (201 Created)
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message", containsString("successful")));

                // Second registration with same data - Should fail (409 Conflict)
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registrationRequest)))
                                .andExpect(status().isConflict());

                // Registration with forbidden role (e.g. trying to register as ORG_ADMIN)
                RegistrationRequestDTO forbiddenRoleRequest = new RegistrationRequestDTO("hacker@enterprise.com",
                                "pass123", "ORG_ADMIN", "enterprise");
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(forbiddenRoleRequest)))
                                .andExpect(status().isBadRequest());

                System.out.println("CUSTOMER registered. Conflict and Forbidden Role cases verified.");
        }

        @Test
        @Order(10)
        @DisplayName("8. Logout ORG_ADMIN")
        void logoutOrgAdmin() throws Exception {
                RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO(orgAdminRefreshToken);

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer " + orgAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("Logged out successfully")));

                System.out.println("ORG_ADMIN logged out successfully.");
        }

        @Test
        @Order(11)
        @DisplayName("9. Organization Inactive / Active Cycle")
        void organizationLifecycleTest() throws Exception {
                // 9.1. Login SUPER_ADMIN to get a fresh token (previous was blacklisted)
                LoginRequestDTO superAdminLogin = new LoginRequestDTO("systesting", "superadmin@superemail.com",
                                "superadminpassword");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(superAdminLogin)))
                                .andReturn();
                String freshSuperToken = objectMapper
                                .readValue(loginResult.getResponse().getContentAsString(), LoginResponseDTO.class)
                                .getAccessToken();

                // 9.2. Deactivate Organization
                UpdateOrganizationStatusRequestDTO deactivate = new UpdateOrganizationStatusRequestDTO();
                deactivate.setIsActive(false);
                mockMvc.perform(patch("/api/super-admin/organizations/" + enterpriseOrgId + "/status")
                                .header("Authorization", "Bearer " + freshSuperToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deactivate)))
                                .andExpect(status().isOk());

                // 9.3. Attempt ORG_ADMIN Login - Should fail with 409 Conflict (as per
                // GlobalExceptionHandler)
                LoginRequestDTO orgAdminLogin = new LoginRequestDTO("enterprise", "vpsciencememories@gmail.com",
                                "orgadminpassword");
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orgAdminLogin)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error", is("Conflict")))
                                .andExpect(jsonPath("$.message", containsString("inactive")));

                // 9.4. Reactivate Organization
                UpdateOrganizationStatusRequestDTO activate = new UpdateOrganizationStatusRequestDTO();
                activate.setIsActive(true);
                mockMvc.perform(patch("/api/super-admin/organizations/" + enterpriseOrgId + "/status")
                                .header("Authorization", "Bearer " + freshSuperToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(activate)))
                                .andExpect(status().isOk());

                // 9.5. Login ORG_ADMIN again - Should work
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orgAdminLogin)))
                                .andExpect(status().isOk());

                System.out.println("Organization lifecycle (Deactivate/Reactivate) verified.");
        }

        @Test
        @Order(12)
        @DisplayName("10. Refresh Token - Negative Cases")
        void refreshTokenNegative() throws Exception {
                // Invalid Refresh Token
                RefreshTokenRequestDTO invalidRequest = new RefreshTokenRequestDTO("invalid-refresh-token");
                mockMvc.perform(post("/api/auth/refresh")
                                .header("Authorization", "Bearer some-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isForbidden());

                System.out.println("Invalid refresh token case verified.");
        }

        @Test
        @Order(13)
        @DisplayName("11. Account Inactive - Login and Refresh Failures")
        void accountInactiveTest() throws Exception {
                // 11.1. Login SUPER_ADMIN to get a fresh token
                LoginRequestDTO superAdminLogin = new LoginRequestDTO("systesting", "superadmin@superemail.com",
                                "superadminpassword");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(superAdminLogin)))
                                .andReturn();
                String freshSuperToken = objectMapper
                                .readValue(loginResult.getResponse().getContentAsString(), LoginResponseDTO.class)
                                .getAccessToken();

                // 11.2. Get ORG_ADMIN's userId
                LoginRequestDTO orgAdminLogin = new LoginRequestDTO("enterprise", "vpsciencememories@gmail.com",
                                "orgadminpassword");
                MvcResult orgLoginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orgAdminLogin)))
                                .andReturn();
                LoginResponseDTO orgResponse = objectMapper.readValue(orgLoginResult.getResponse().getContentAsString(),
                                LoginResponseDTO.class);
                String currentOrgAdminAccessToken = orgResponse.getAccessToken();
                String currentOrgAdminRefreshToken = orgResponse.getRefreshToken();

                // Find org admin UUID
                MvcResult listResult = mockMvc.perform(get("/api/super-admin/org-admins")
                                .header("Authorization", "Bearer " + freshSuperToken))
                                .andReturn();
                UserResponseDTO[] admins = objectMapper.treeToValue(
                                objectMapper.readTree(listResult.getResponse().getContentAsString()).get("orgAdmins"),
                                UserResponseDTO[].class);
                UUID orgAdminId = null;
                for (UserResponseDTO admin : admins) {
                        if (admin.getEmail().equals("vpsciencememories@gmail.com")) {
                                orgAdminId = admin.getUserId();
                                break;
                        }
                }
                Assertions.assertNotNull(orgAdminId, "ORG_ADMIN should exist before status update");

                // 11.3. Deactivate ORG_ADMIN account
                UpdateUserStatusRequestDTO deactivate = new UpdateUserStatusRequestDTO();
                deactivate.setIsActive(false);
                mockMvc.perform(patch("/api/super-admin/org-admins/" + orgAdminId + "/status")
                                .header("Authorization", "Bearer " + freshSuperToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deactivate)))
                                .andExpect(status().isOk());

                // 11.4. Attempt Login - Should fail (401)
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orgAdminLogin)))
                                .andExpect(status().isUnauthorized());

                // 11.5. Attempt Refresh Token - Should fail (401)
                RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(currentOrgAdminRefreshToken);
                mockMvc.perform(post("/api/auth/refresh")
                                .header("Authorization", "Bearer " + currentOrgAdminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                                .andExpect(status().isUnauthorized());

                // Restore shared state for later integration files in the documented flow.
                UpdateUserStatusRequestDTO reactivate = new UpdateUserStatusRequestDTO();
                reactivate.setIsActive(true);
                mockMvc.perform(patch("/api/super-admin/org-admins/" + orgAdminId + "/status")
                                .header("Authorization", "Bearer " + freshSuperToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reactivate)))
                                .andExpect(status().isOk());

                System.out.println("Account inactive (Login/Refresh blocking) verified.");
        }
}