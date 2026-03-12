package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.ForgotPasswordRequestDTO;
import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.ResetPasswordRequestDTO;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.EmailService;
import com.example.ordermgmt.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.CommonCachesTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@RecordApplicationEvents
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
class CustomerAuthLifeIntegrationTest {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ORG_SUBDOMAIN = "acme";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private InMemoryStringRedisTemplate stringRedisTemplate;

    @Autowired
    private CapturingEmailService emailService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        stringRedisTemplate.clearStore();
        emailService.clear();
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
        organizationRepository.deleteAll();
        userRoleRepository.deleteAll();
        seedRole(1, ROLE_CUSTOMER);
    }

    @Test
    void sq15_sq16_registerCustomer_createsAppUserAndEmptyCustomerProfile() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);

        RegistrationRequestDTO request = new RegistrationRequestDTO(
                CUSTOMER_EMAIL,
                "secret123",
                ROLE_CUSTOMER,
                organization.getSubdomain());

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));

        AppUser user = appUserRepository.findByOrgIdAndEmailIgnoreCase(organization.getOrgId(), CUSTOMER_EMAIL)
                .orElseThrow();
        Customer customer = customerRepository.findByAppUser(user).orElseThrow();

        assertThat(user.getRole().getRoleName()).isEqualTo(ROLE_CUSTOMER);
        assertThat(user.getOrgId()).isEqualTo(organization.getOrgId());
        assertThat(user.getIsActive()).isTrue();
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();
        assertThat(customer.getOrgId()).isEqualTo(organization.getOrgId());
        assertThat(customer.getAppUser().getEmail()).isEqualTo(CUSTOMER_EMAIL);
    }

    @Test
    void sq15_negative_registerWithNonCustomerRole_returnsForbidden() throws Exception {
        saveOrganization(ORG_SUBDOMAIN, true);

        RegistrationRequestDTO request = new RegistrationRequestDTO(
                CUSTOMER_EMAIL,
                "secret123",
                "ADMIN",
                ORG_SUBDOMAIN);

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "10.0.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only CUSTOMER registration is allowed on this endpoint"));

        assertThat(appUserRepository.findAll()).isEmpty();
        assertThat(customerRepository.findAll()).isEmpty();
    }

    @Test
    void sq17_login_returnsJwtAndStoresRefreshToken() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);
        saveCustomerUser(CUSTOMER_EMAIL, "secret123", organization.getOrgId(), true);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(
                                organization.getSubdomain(),
                                CUSTOMER_EMAIL,
                                "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.role").value(ROLE_CUSTOMER))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String refreshToken = body.get("refreshToken").asText();

        assertThat(stringRedisTemplate.getValue("RT:" + refreshToken)).isNotBlank();
    }

    @Test
    void sq18_refresh_rotatesRefreshTokenAndBlacklistsOldAccessToken() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);
        saveCustomerUser(CUSTOMER_EMAIL, "secret123", organization.getOrgId(), true);

        JsonNode login = loginAndReadJson(organization.getSubdomain(), CUSTOMER_EMAIL, "secret123");
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        JsonNode refreshedBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshedBody.get("refreshToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(refreshToken);
        assertThat(stringRedisTemplate.hasKey("RT:" + refreshToken)).isFalse();
        assertThat(stringRedisTemplate.getValue("RT:" + newRefreshToken)).isNotBlank();
        assertThat(tokenBlacklistService.isBlacklisted(accessToken)).isTrue();
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, authorities = ROLE_CUSTOMER)
    void sq19_logout_deletesRefreshTokenAndBlacklistsAccessToken() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);
        saveCustomerUser(CUSTOMER_EMAIL, "secret123", organization.getOrgId(), true);

        JsonNode login = loginAndReadJson(organization.getSubdomain(), CUSTOMER_EMAIL, "secret123");
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertThat(stringRedisTemplate.hasKey("RT:" + refreshToken)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(accessToken)).isTrue();
    }

    @Test
    void sq20_sq21_forgotAndResetPassword_updatesPasswordAndAllowsRelogin() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);
        AppUser user = saveCustomerUser(CUSTOMER_EMAIL, "secret123", organization.getOrgId(), true);
        String oldPasswordHash = user.getPasswordHash();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("X-Forwarded-For", "10.0.0.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequestDTO(
                                CUSTOMER_EMAIL,
                                organization.getSubdomain()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account matches, a temporary password has been sent to your email."));

        String resetKey = "PR:" + organization.getOrgId() + ":" + CUSTOMER_EMAIL;
        assertThat(stringRedisTemplate.getValue(resetKey)).isNotBlank();

        EmailDispatchEvent resetEvent = applicationEvents.stream(EmailDispatchEvent.class)
                .reduce((first, second) -> second)
                .orElseThrow();
        String temporaryPassword = (String) resetEvent.templateData().get("tempPassword");

        mockMvc.perform(patch("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequestDTO(
                                CUSTOMER_EMAIL,
                                organization.getSubdomain(),
                                temporaryPassword,
                                "newSecret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been successfully reset."));

        AppUser updatedUser = appUserRepository.findByEmail(CUSTOMER_EMAIL).orElseThrow();
        assertThat(updatedUser.getPasswordHash()).isNotEqualTo(oldPasswordHash);
        assertThat(passwordEncoder.matches("newSecret123", updatedUser.getPasswordHash())).isTrue();
        assertThat(updatedUser.getIsPasswordChanged()).isTrue();
        assertThat(stringRedisTemplate.hasKey(resetKey)).isFalse();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(
                                organization.getSubdomain(),
                                CUSTOMER_EMAIL,
                                "newSecret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void sq24_loginWithWrongPassword_returnsUnauthorized() throws Exception {
        Organization organization = saveOrganization(ORG_SUBDOMAIN, true);
        saveCustomerUser(CUSTOMER_EMAIL, "secret123", organization.getOrgId(), true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(
                                organization.getSubdomain(),
                                CUSTOMER_EMAIL,
                                "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    private JsonNode loginAndReadJson(String orgSubdomain, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(
                                orgSubdomain,
                                email,
                                password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Organization saveOrganization(String subdomain, boolean isActive) {
        Organization organization = new Organization();
        organization.setOrgId(UUID.randomUUID());
        organization.setName("Org for " + subdomain);
        organization.setSubdomain(subdomain);
        organization.setDescription("Seeded organization for auth lifecycle tests");
        organization.setIsActive(isActive);
        return organizationRepository.save(organization);
    }

    private AppUser saveCustomerUser(String email, String rawPassword, UUID orgId, boolean isActive) {
        UserRole role = userRoleRepository.findByRoleName(ROLE_CUSTOMER).orElseThrow();

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setOrgId(orgId);
        user.setIsActive(isActive);
        user.setIsPasswordChanged(false);
        return appUserRepository.save(user);
    }

    private void seedRole(int roleId, String roleName) {
        UserRole role = new UserRole();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        userRoleRepository.save(role);
    }

    @TestConfiguration
    static class AuthTestConfig {

        @Bean(name = "taskExecutor")
        Executor taskExecutor() {
            return Runnable::run;
        }

        @Bean
        @Primary
        InMemoryStringRedisTemplate stringRedisTemplate() {
            return new InMemoryStringRedisTemplate();
        }

        @Bean
        @Primary
        CapturingEmailService emailService() {
            return new CapturingEmailService();
        }
    }

    static class CapturingEmailService implements EmailService {

        private volatile String lastRecipient;
        private volatile String lastSubject;
        private volatile String lastBody;

        @Override
        public void sendEmail(String to, String subject, String body) {
            this.lastRecipient = to;
            this.lastSubject = subject;
            this.lastBody = body;
        }

        void clear() {
            this.lastRecipient = null;
            this.lastSubject = null;
            this.lastBody = null;
        }

        String lastRecipient() {
            return lastRecipient;
        }

        String lastSubject() {
            return lastSubject;
        }

        String lastBody() {
            return lastBody;
        }
    }

    static class InMemoryStringRedisTemplate extends StringRedisTemplate {

        private final Map<String, RedisEntry> store = new ConcurrentHashMap<>();
        private final ValueOperations<String, String> valueOperations = createValueOperationsProxy();

        @Override
        public void afterPropertiesSet() {
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }

        @Override
        public Boolean delete(String key) {
            purgeExpired(key);
            return store.remove(key) != null;
        }

        @Override
        public Boolean hasKey(String key) {
            purgeExpired(key);
            return store.containsKey(key);
        }

        @Override
        public Boolean expire(String key, long timeout, TimeUnit unit) {
            purgeExpired(key);
            RedisEntry current = store.get(key);
            if (current == null) {
                return false;
            }
            store.put(key, new RedisEntry(current.value(), Instant.now().plusMillis(unit.toMillis(timeout))));
            return true;
        }

        void clearStore() {
            store.clear();
        }

        String getValue(String key) {
            purgeExpired(key);
            RedisEntry entry = store.get(key);
            return entry != null ? entry.value() : null;
        }

        private ValueOperations<String, String> createValueOperationsProxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if ("set".equals(name)) {
                    handleSet(args);
                    return null;
                }
                if ("get".equals(name)) {
                    return getValue((String) args[0]);
                }
                if ("increment".equals(name)) {
                    return handleIncrement(args);
                }
                if ("setIfAbsent".equals(name)) {
                    if (hasKey((String) args[0])) {
                        return false;
                    }
                    handleSet(args);
                    return true;
                }
                if ("setIfPresent".equals(name)) {
                    if (!hasKey((String) args[0])) {
                        return false;
                    }
                    handleSet(args);
                    return true;
                }
                if ("getAndDelete".equals(name)) {
                    String key = (String) args[0];
                    String current = getValue(key);
                    delete(key);
                    return current;
                }
                if ("getAndSet".equals(name)) {
                    String key = (String) args[0];
                    String current = getValue(key);
                    store.put(key, new RedisEntry((String) args[1], null));
                    return current;
                }
                if ("getAndExpire".equals(name)) {
                    String key = (String) args[0];
                    String current = getValue(key);
                    if (current == null) {
                        return null;
                    }
                    if (args[1] instanceof Duration duration) {
                        expire(key, duration.toMillis(), TimeUnit.MILLISECONDS);
                    } else {
                        expire(key, (Long) args[1], (TimeUnit) args[2]);
                    }
                    return current;
                }
                if ("getAndPersist".equals(name)) {
                    String key = (String) args[0];
                    String current = getValue(key);
                    if (current != null) {
                        store.put(key, new RedisEntry(current, null));
                    }
                    return current;
                }
                if ("multiSet".equals(name)) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> entries = (Map<String, String>) args[0];
                    entries.forEach((key, value) -> store.put(key, new RedisEntry(value, null)));
                    return null;
                }
                if ("multiGet".equals(name)) {
                    @SuppressWarnings("unchecked")
                    Collection<String> keys = (Collection<String>) args[0];
                    return keys.stream().map(this::getValue).toList();
                }
                if ("size".equals(name)) {
                    String value = getValue((String) args[0]);
                    return value != null ? (long) value.length() : 0L;
                }
                if ("append".equals(name)) {
                    String key = (String) args[0];
                    String suffix = (String) args[1];
                    String current = getValue(key);
                    String updated = (current == null ? "" : current) + suffix;
                    store.put(key, new RedisEntry(updated, null));
                    return updated.length();
                }
                if ("getOperations".equals(name)) {
                    return this;
                }
                if ("decrement".equals(name)) {
                    long delta = args.length == 1 ? 1L : (Long) args[1];
                    return handleIncrement(new Object[] { args[0], -delta });
                }
                if ("setBit".equals(name) || "getBit".equals(name) || "bitField".equals(name)
                        || "get".equals(name) && args.length == 3) {
                    throw new UnsupportedOperationException("Bit/range operations are not needed in auth tests");
                }
                throw new UnsupportedOperationException("Unsupported Redis operation: " + name);
            };

            @SuppressWarnings("unchecked")
            ValueOperations<String, String> proxy = (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[] { ValueOperations.class },
                    handler);
            return proxy;
        }

        private void handleSet(Object[] args) {
            String key = (String) args[0];
            String value = (String) args[1];
            Instant expiresAt = null;

            if (args.length == 3 && args[2] instanceof Duration duration) {
                expiresAt = Instant.now().plus(duration);
            } else if (args.length >= 4 && args[2] instanceof Long timeout && args[3] instanceof TimeUnit unit) {
                expiresAt = Instant.now().plusMillis(unit.toMillis(timeout));
            }

            store.put(key, new RedisEntry(value, expiresAt));
        }

        private Long handleIncrement(Object[] args) {
            String key = (String) args[0];
            long delta = 1L;
            if (args.length > 1) {
                Object secondArg = args[1];
                if (secondArg instanceof Long longDelta) {
                    delta = longDelta;
                } else if (secondArg instanceof Double doubleDelta) {
                    delta = (long) doubleDelta.doubleValue();
                }
            }

            purgeExpired(key);
            RedisEntry current = store.get(key);
            long currentValue = current == null ? 0L : Long.parseLong(current.value());
            long updated = currentValue + delta;
            Instant expiresAt = current != null ? current.expiresAt() : null;
            store.put(key, new RedisEntry(Long.toString(updated), expiresAt));
            return updated;
        }

        private void purgeExpired(String key) {
            RedisEntry entry = store.get(key);
            if (entry != null && entry.expiresAt() != null && Instant.now().isAfter(entry.expiresAt())) {
                store.remove(key);
            }
        }

        private record RedisEntry(String value, Instant expiresAt) {
        }
    }
}
