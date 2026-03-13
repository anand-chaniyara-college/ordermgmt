package com.example.ordermgmt.integration.old;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.UpdateOrganizationStatusRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class SuperAdminBootstrapIntegrationTest {

    private static final String SUPER_ADMIN_EMAIL = "super-admin@example.com";
    private static final String ROLE_ORG_ADMIN = "ORG_ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

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
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq01_createOrganization_persistsActiveOrganization() throws Exception {
        CreateOrganizationRequestDTO request = new CreateOrganizationRequestDTO(
                "Acme Corporation",
                "acme",
                "Primary tenant for bootstrap flow");

        MvcResult result = mockMvc.perform(post("/api/super-admin/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.subdomain").value("acme"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID orgId = UUID.fromString(body.get("orgId").asText());

        Organization organization = organizationRepository.findById(orgId).orElseThrow();
        assertThat(organization.getName()).isEqualTo("Acme Corporation");
        assertThat(organization.getSubdomain()).isEqualTo("acme");
        assertThat(organization.getDescription()).isEqualTo("Primary tenant for bootstrap flow");
        assertThat(organization.getIsActive()).isTrue();
        assertThat(organization.getCreatedBy()).isEqualTo(SUPER_ADMIN_EMAIL);
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq02_sq03_listOrganizations_returnsBothCreatedOrganizations() throws Exception {
        organizationRepository.save(buildOrganization("Org Alpha", "org-alpha", true));
        organizationRepository.save(buildOrganization("Org Beta", "org-beta", true));

        mockMvc.perform(get("/api/super-admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizations.length()").value(2))
                .andExpect(jsonPath("$.organizations[0].subdomain").exists())
                .andExpect(jsonPath("$.organizations[1].subdomain").exists());

        List<String> subdomains = organizationRepository.findAll().stream()
                .map(Organization::getSubdomain)
                .sorted()
                .toList();
        assertThat(subdomains).containsExactly("org-alpha", "org-beta");
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq04_sq05_createOrgAdmins_persistsUsersInTheirTargetOrganizations() throws Exception {
        Organization orgA = organizationRepository.save(buildOrganization("Org Alpha", "org-alpha", true));
        Organization orgB = organizationRepository.save(buildOrganization("Org Beta", "org-beta", true));

        createOrgAdmin("org-admin-a@example.com", "secret123", orgA.getOrgId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("org-admin-a@example.com"))
                .andExpect(jsonPath("$.role").value(ROLE_ORG_ADMIN));

        createOrgAdmin("org-admin-b@example.com", "secret456", orgB.getOrgId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("org-admin-b@example.com"))
                .andExpect(jsonPath("$.role").value(ROLE_ORG_ADMIN));

        AppUser orgAdminA = appUserRepository.findByEmail("org-admin-a@example.com").orElseThrow();
        AppUser orgAdminB = appUserRepository.findByEmail("org-admin-b@example.com").orElseThrow();

        assertThat(orgAdminA.getOrgId()).isEqualTo(orgA.getOrgId());
        assertThat(orgAdminB.getOrgId()).isEqualTo(orgB.getOrgId());
        assertThat(orgAdminA.getRole().getRoleName()).isEqualTo(ROLE_ORG_ADMIN);
        assertThat(orgAdminB.getRole().getRoleName()).isEqualTo(ROLE_ORG_ADMIN);
        assertThat(passwordEncoder.matches("secret123", orgAdminA.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("secret456", orgAdminB.getPasswordHash())).isTrue();
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq06_updateOrgAdminStatus_updatesPersistedActiveFlag() throws Exception {
        Organization orgA = organizationRepository.save(buildOrganization("Org Alpha", "org-alpha", true));
        AppUser orgAdmin = saveOrgAdmin("org-admin-a@example.com", orgA.getOrgId(), true);

        mockMvc.perform(patch("/api/super-admin/org-admins/{id}/status", orgAdmin.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDTO(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Org Admin status updated successfully."));

        AppUser updatedUser = appUserRepository.findById(orgAdmin.getUserId()).orElseThrow();
        assertThat(updatedUser.getIsActive()).isFalse();
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq07_updateOrganizationStatus_updatesPersistedActiveFlag() throws Exception {
        Organization orgB = organizationRepository.save(buildOrganization("Org Beta", "org-beta", true));

        mockMvc.perform(patch("/api/super-admin/organizations/{id}/status", orgB.getOrgId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateOrganizationStatusRequestDTO(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organization status updated successfully."));

        Organization updatedOrg = organizationRepository.findById(orgB.getOrgId()).orElseThrow();
        assertThat(updatedOrg.getIsActive()).isFalse();
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq01_negative_createOrganization_withDuplicateSubdomain_returnsConflictAndKeepsSingleRow() throws Exception {
        organizationRepository.save(buildOrganization("Existing Org", "acme", true));

        CreateOrganizationRequestDTO duplicateRequest = new CreateOrganizationRequestDTO(
                "Acme Clone",
                "acme",
                "Conflicting subdomain");

        mockMvc.perform(post("/api/super-admin/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Organization subdomain already exists: acme"));

        assertThat(organizationRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(username = SUPER_ADMIN_EMAIL, authorities = "SUPER_ADMIN")
    void sq04_negative_createOrgAdmin_withMissingOrgId_returnsBadRequestAndKeepsUsersUnchanged() throws Exception {
        CreateOrgAdminRequestDTO request = new CreateOrgAdminRequestDTO(
                "org-admin@example.com",
                "secret123",
                null);

        mockMvc.perform(post("/api/super-admin/org-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("orgId is required"));

        assertThat(appUserRepository.findAll()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions createOrgAdmin(String email, String password, UUID orgId)
            throws Exception {
        CreateOrgAdminRequestDTO request = new CreateOrgAdminRequestDTO(email, password, orgId);
        return mockMvc.perform(post("/api/super-admin/org-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Organization buildOrganization(String name, String subdomain, boolean isActive) {
        Organization organization = new Organization();
        organization.setOrgId(UUID.randomUUID());
        organization.setName(name);
        organization.setSubdomain(subdomain);
        organization.setDescription("Seeded test organization for " + subdomain);
        organization.setIsActive(isActive);
        return organization;
    }

    private AppUser saveOrgAdmin(String email, UUID orgId, boolean isActive) {
        UserRole orgAdminRole = userRoleRepository.findByRoleName(ROLE_ORG_ADMIN).orElseThrow();

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setRole(orgAdminRole);
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
