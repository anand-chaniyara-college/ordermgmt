package com.example.ordermgmt.config;

import com.example.ordermgmt.security.TenantIdentifierResolver;
import java.util.Map;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernateTenantCustomizer(TenantIdentifierResolver tenantIdentifierResolver) {
        return (Map<String, Object> properties) -> {
            properties.put("hibernate.multiTenancy", "DISCRIMINATOR");
            properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        };
    }
}
