package com.example.ordermgmt.security;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContextHolder.getTenantId();
        return tenantId != null ? tenantId : TenantContextHolder.ROOT_TENANT_ID;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return TenantContextHolder.ROOT_TENANT_ID.equals(tenantId);
    }
}
