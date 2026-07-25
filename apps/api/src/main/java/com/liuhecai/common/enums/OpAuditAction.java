package com.liuhecai.common.enums;

public final class OpAuditAction {
    public static final String USER_ENABLE = "USER_ENABLE";
    public static final String USER_DISABLE = "USER_DISABLE";
    public static final String USER_RESET_PASSWORD = "USER_RESET_PASSWORD";
    public static final String USER_BATCH_DISABLE = "USER_BATCH_DISABLE";
    public static final String USER_FORCE_LOGOUT = "USER_FORCE_LOGOUT";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String AGENT_ENABLE = "AGENT_ENABLE";
    public static final String AGENT_DISABLE = "AGENT_DISABLE";
    public static final String AGENT_RESET_PASSWORD = "AGENT_RESET_PASSWORD";
    public static final String AGENT_FORCE_LOGOUT = "AGENT_FORCE_LOGOUT";
    public static final String AGENT_CREATE = "AGENT_CREATE";
    public static final String AGENT_DELETE = "AGENT_DELETE";
    public static final String AGENT_SET_PRIMARY = "AGENT_SET_PRIMARY";
    public static final String TENANT_UPDATE = "TENANT_UPDATE";
    public static final String USER_COIN_ADJUST = "USER_COIN_ADJUST";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    private OpAuditAction() {
    }
}
