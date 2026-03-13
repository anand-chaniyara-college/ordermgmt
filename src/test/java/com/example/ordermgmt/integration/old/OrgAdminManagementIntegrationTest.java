package com.example.ordermgmt.integration.old;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.CommonCachesTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners({
        ServletTestExecutionListener.class,
        DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class,
        BeanOverrideTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        CommonCachesTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class,
        EventPublishingTestExecutionListener.class
})
class OrgAdminManagementIntegrationTest {

    private static final String ROLE_ORG_ADMIN = "ORG_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String REQUESTER_EMAIL = "org-admin-a@example.com";
    private static final String OTHER_ORG_ADMIN_EMAIL = "org-admin-b@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        appUserRepository.deleteAll();
        organizationRepository.deleteAll();
        userRoleRepository.deleteAll();
        seedRole(1, ROLE_ORG_ADMIN);
        seedRole(2, ROLE_ADMIN);
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq08_createAdmin_persistsAdminInRequesterOrganization() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        saveUser(REQUESTER_EMAIL, ROLE_ORG_ADMIN, orgA.getOrgId(), true);

        CreateAdminRequestDTO request = new CreateAdminRequestDTO("admin-a1@example.com", "secret123");

        mockMvc.perform(post("/api/org-admin/admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin-a1@example.com"))
                .andExpect(jsonPath("$.role").value(ROLE_ADMIN))
                .andExpect(jsonPath("$.orgId").value(orgA.getOrgId().toString()))
                .andExpect(jsonPath("$.isActive").value(true));

        AppUser admin = appUserRepository.findByEmail("admin-a1@example.com").orElseThrow();
        assertThat(admin.getOrgId()).isEqualTo(orgA.getOrgId());
        assertThat(admin.getRole().getRoleName()).isEqualTo(ROLE_ADMIN);
        assertThat(passwordEncoder.matches("secret123", admin.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq09_sq10_listAdmins_returnsOnlyRequesterOrganizationAdmins() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        Organization orgB = saveOrganization("Org Beta", "org-beta");
        saveUser(REQUESTER_EMAIL, ROLE_ORG_ADMIN, orgA.getOrgId(), true);
        saveUser("admin-a1@example.com", ROLE_ADMIN, orgA.getOrgId(), true);
        saveUser("admin-a2@example.com", ROLE_ADMIN, orgA.getOrgId(), false);
        saveUser(OTHER_ORG_ADMIN_EMAIL, ROLE_ORG_ADMIN, orgB.getOrgId(), true);
        saveUser("admin-b1@example.com", ROLE_ADMIN, orgB.getOrgId(), true);

        mockMvc.perform(get("/api/org-admin/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admins.length()").value(2))
                .andExpect(jsonPath("$.admins[0].email").exists())
                .andExpect(jsonPath("$.admins[1].email").exists());

        List<String> adminEmails = appUserRepository.findByRole_RoleNameAndOrgId(ROLE_ADMIN, orgA.getOrgId()).stream()
                .map(AppUser::getEmail)
                .sorted()
                .toList();
        assertThat(adminEmails).containsExactly("admin-a1@example.com", "admin-a2@example.com");
        assertThat(adminEmails).doesNotContain("admin-b1@example.com");
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq11_updateAdminStatus_updatesPersistedFlagForSameOrganizationAdmin() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        saveUser(REQUESTER_EMAIL, ROLE_ORG_ADMIN, orgA.getOrgId(), true);
        AppUser admin = saveUser("admin-a1@example.com", ROLE_ADMIN, orgA.getOrgId(), true);

        mockMvc.perform(patch("/api/org-admin/admins/{id}/status", admin.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin status updated successfully."));

        AppUser updatedAdmin = appUserRepository.findById(admin.getUserId()).orElseThrow();
        assertThat(updatedAdmin.getIsActive()).isFalse();
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq12_createAdmin_withDuplicateEmailInSameOrganization_returnsConflict() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        saveUser(REQUESTER_EMAIL, ROLE_ORG_ADMIN, orgA.getOrgId(), true);
        saveUser("admin-a1@example.com", ROLE_ADMIN, orgA.getOrgId(), true);

        CreateAdminRequestDTO duplicateRequest = new CreateAdminRequestDTO("admin-a1@example.com", "secret123");

        mockMvc.perform(post("/api/org-admin/admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: admin-a1@example.com"));

        assertThat(appUserRepository.findByRole_RoleNameAndOrgId(ROLE_ADMIN, orgA.getOrgId())).hasSize(1);
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq13_updateAdminStatus_forCrossOrganizationAdmin_returnsBadRequestAndKeepsTargetUnchanged() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        Organization orgB = saveOrganization("Org Beta", "org-beta");
        saveUser(REQUESTER_EMAIL, ROLE_ORG_ADMIN, orgA.getOrgId(), true);
        AppUser foreignAdmin = saveUser("admin-b1@example.com", ROLE_ADMIN, orgB.getOrgId(), true);

        mockMvc.perform(patch("/api/org-admin/admins/{id}/status", foreignAdmin.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cross-organization admin update is not allowed"));

        AppUser reloadedAdmin = appUserRepository.findById(foreignAdmin.getUserId()).orElseThrow();
        assertThat(reloadedAdmin.getIsActive()).isTrue();
    }

    @Test
    @WithMockUser(username = REQUESTER_EMAIL, authorities = ROLE_ORG_ADMIN)
    void sq14_createAdmin_withRequesterNotMarkedAsOrgAdminInDatabase_returnsBadRequest() throws Exception {
        Organization orgA = saveOrganization("Org Alpha", "org-alpha");
        saveUser(REQUESTER_EMAIL, ROLE_ADMIN, orgA.getOrgId(), true);

        CreateAdminRequestDTO request = new CreateAdminRequestDTO("admin-a1@example.com", "secret123");

        mockMvc.perform(post("/api/org-admin/admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requester is not ORG_ADMIN"));

        assertThat(appUserRepository.findByEmail("admin-a1@example.com")).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin-user@example.com", authorities = ROLE_ADMIN)
    void sq14_security_userWithoutOrgAdminAuthority_isForbidden() throws Exception {
        CreateAdminRequestDTO request = new CreateAdminRequestDTO("admin-a1@example.com", "secret123");

        mockMvc.perform(post("/api/org-admin/admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Organization saveOrganization(String name, String subdomain) {
        Organization organization = new Organization();
        organization.setOrgId(UUID.randomUUID());
        organization.setName(name);
        organization.setSubdomain(subdomain);
        organization.setDescription("Seeded organization for " + subdomain);
        organization.setIsActive(true);
        return organizationRepository.save(organization);
    }

    private AppUser saveUser(String email, String roleName, UUID orgId, boolean isActive) {
        UserRole role = userRoleRepository.findByRoleName(roleName).orElseThrow();

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setRole(role);
        user.setOrgId(orgId);
        user.setIsActive(isActive);
        return appUserRepository.save(user);
    }

    private void seedRole(int roleId, String roleName) {
        UserRole role = new UserRole();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        userRoleRepository.save(role);
    }

    @TestConfiguration
    static class TestEmailConfig {

        @Bean
        @Primary
        EmailService emailService() {
            return (to, subject, body) -> {
            };
        }
    }
}
