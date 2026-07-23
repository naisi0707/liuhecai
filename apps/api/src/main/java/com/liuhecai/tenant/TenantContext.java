package com.liuhecai.tenant;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> HOST_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    public static Long get() {
        return TENANT_HOLDER.get();
    }

    public static void setHost(String host) {
        HOST_HOLDER.set(host);
    }

    public static String getHost() {
        return HOST_HOLDER.get();
    }

    public static void clear() {
        TENANT_HOLDER.remove();
        HOST_HOLDER.remove();
    }
}
