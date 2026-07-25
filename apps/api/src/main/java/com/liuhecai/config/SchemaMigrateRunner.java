package com.liuhecai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SchemaMigrateRunner implements ApplicationRunner {

    private static final String[] ADD_TOKEN_VERSION = {
            "ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0",
            "ALTER TABLE agent_accounts ADD COLUMN token_version INT NOT NULL DEFAULT 0",
            "ALTER TABLE super_admins ADD COLUMN token_version INT NOT NULL DEFAULT 0"
    };

    private static final String CREATE_OP_AUDIT_LOGS = """
            CREATE TABLE IF NOT EXISTS op_audit_logs (
              id BIGINT PRIMARY KEY,
              operator_realm VARCHAR(16) NOT NULL,
              operator_id BIGINT NOT NULL,
              operator_name VARCHAR(64) NOT NULL,
              tenant_id BIGINT NULL,
              action VARCHAR(64) NOT NULL,
              target_type VARCHAR(32) NOT NULL,
              target_id VARCHAR(64) NULL,
              detail VARCHAR(1024) NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_op_audit_created (created_at),
              KEY idx_op_audit_action (action),
              KEY idx_op_audit_operator (operator_realm, operator_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String CREATE_IP_WHITELIST_SETTINGS = """
            CREATE TABLE IF NOT EXISTS ip_whitelist_settings (
              id BIGINT PRIMARY KEY,
              enabled TINYINT NOT NULL DEFAULT 0 COMMENT '0关闭 1启用',
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String CREATE_IP_WHITELIST_ENTRIES = """
            CREATE TABLE IF NOT EXISTS ip_whitelist_entries (
              id BIGINT PRIMARY KEY,
              cidr VARCHAR(64) NOT NULL,
              note VARCHAR(128) NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_ip_whitelist_cidr (cidr)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String CREATE_ENTRY_LINES = """
            CREATE TABLE IF NOT EXISTS entry_lines (
              id BIGINT PRIMARY KEY,
              entry_domain_id BIGINT NOT NULL COMMENT 'domains.id role=ENTRY',
              sort_order INT NOT NULL DEFAULT 0,
              label VARCHAR(64) NOT NULL,
              color VARCHAR(16) NOT NULL DEFAULT '#c62828',
              target_tenant_id BIGINT NOT NULL,
              status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              KEY idx_entry_lines_domain (entry_domain_id, sort_order)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String[] PERF_INDEXES = {
            "ALTER TABLE users ADD INDEX idx_users_tenant_created (tenant_id, created_at)",
            "ALTER TABLE users ADD INDEX idx_users_created (created_at)",
            "ALTER TABLE topic_orders ADD INDEX idx_topic_orders_tenant_created (tenant_id, created_at)",
            "ALTER TABLE topic_orders ADD INDEX idx_topic_orders_created (created_at)",
            "ALTER TABLE recharge_requests ADD INDEX idx_recharge_tenant_status_handled (tenant_id, status, handled_at)",
            "ALTER TABLE topics ADD INDEX idx_topics_tenant_status_created (tenant_id, status, created_at)",
            "ALTER TABLE topics ADD INDEX idx_topics_updated (updated_at)"
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String ddl : ADD_TOKEN_VERSION) {
            executeIdempotent(ddl);
        }
        // M7 optimistic lock column required by User.@Version / coin adjust
        executeIdempotent(
                "ALTER TABLE users ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER coin_balance");
        executeIdempotent(CREATE_OP_AUDIT_LOGS);
        executeIdempotent(CREATE_IP_WHITELIST_SETTINGS);
        executeIdempotent(CREATE_IP_WHITELIST_ENTRIES);
        executeIdempotent(CREATE_ENTRY_LINES);
        executeIdempotent(
                "ALTER TABLE domains ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'FORUM' COMMENT 'ENTRY入口伪装 FORUM论坛' AFTER is_primary");
        executeIdempotent(
                "ALTER TABLE topics ADD COLUMN tag VARCHAR(32) NOT NULL DEFAULT '出售帖' COMMENT '帖子标签' AFTER play_type");
        for (String ddl : PERF_INDEXES) {
            executeIdempotent(ddl);
        }

        // M17: primary agent columns (data backfill in AuthSeedRunner; FKs in TenantConstraintFinalizeRunner)
        executeIdempotent(
                "ALTER TABLE agent_accounts ADD COLUMN is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '1主代理' AFTER enabled");
        executeIdempotent(
                "ALTER TABLE agent_accounts ADD COLUMN primary_key TINYINT NULL COMMENT '主代理唯一槽' AFTER is_primary");
        executeIdempotent(
                "ALTER TABLE agent_accounts ADD UNIQUE KEY uk_agent_tenant_primary (tenant_id, primary_key)");
        executeIdempotent(
                "ALTER TABLE tenants ADD COLUMN primary_agent_id BIGINT NULL COMMENT '主代理id' AFTER status");

        executeIdempotent(
                "ALTER TABLE agent_accounts ADD CONSTRAINT fk_agent_tenant "
                        + "FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT ON UPDATE CASCADE");
        executeIdempotent(
                "ALTER TABLE domains ADD CONSTRAINT fk_domain_tenant "
                        + "FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT ON UPDATE CASCADE");

        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ip_whitelist_settings WHERE id = 1", Integer.class);
            if (n == null || n == 0) {
                jdbcTemplate.execute(
                        "INSERT INTO ip_whitelist_settings (id, enabled, updated_at) VALUES (1, 0, NOW())");
            }
        } catch (Exception e) {
            log.debug("Seed ip whitelist settings skip: {}", e.getMessage());
        }

        // drop residual local ENTRY host once real ENTRY (e.g. 157465.com) exists
        try {
            Integer entryProd = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM domains WHERE host IN ('157465.com','www.157465.com') AND role='ENTRY'",
                    Integer.class);
            if (entryProd != null && entryProd > 0) {
                int n = jdbcTemplate.update("DELETE FROM domains WHERE host = 'entry.127.0.0.1'");
                if (n > 0) {
                    log.info("Removed residual domain entry.127.0.0.1");
                }
            }
        } catch (Exception e) {
            log.debug("Cleanup entry.127.0.0.1 skip: {}", e.getMessage());
        }
    }

    private void executeIdempotent(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Schema migrate OK: {}", sql.substring(0, Math.min(sql.length(), 80)));
        } catch (Exception e) {
            if (isAlreadyExists(e)) {
                log.debug("Schema migrate skip (already exists): {}", e.getMessage());
                return;
            }
            throw e;
        }
    }

    private static boolean isAlreadyExists(Throwable e) {
        while (e != null) {
            String message = e.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("duplicate column")
                        || lower.contains("duplicate key name")
                        || lower.contains("duplicate key in table")
                        || lower.contains("can't write; duplicate key")
                        || lower.contains("already exists")
                        || lower.contains("duplicate foreign key")
                        || (lower.contains("can't create") && lower.contains("foreign key"))
                        || lower.contains("errno: 121")) {
                    return true;
                }
            }
            e = e.getCause();
        }
        return false;
    }
}
