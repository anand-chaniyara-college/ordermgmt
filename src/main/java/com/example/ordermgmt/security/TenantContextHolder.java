package com.example.ordermgmt.security;

import java.util.UUID;

public final class TenantContextHolder {

    public static final UUID ROOT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final ThreadLocal<UUID> TENANT_CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    public static UUID getTenantId() {
        return TENANT_CONTEXT.get();
    }

    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}
