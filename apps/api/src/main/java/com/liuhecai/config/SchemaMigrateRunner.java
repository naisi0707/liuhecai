package com.liuhecai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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
        executeIdempotent(CREATE_OP_AUDIT_LOGS);
        executeIdempotent(
                "ALTER TABLE domains ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'FORUM' COMMENT 'ENTRY入口伪装 FORUM论坛' AFTER is_primary");
        executeIdempotent(
                "ALTER TABLE topics ADD COLUMN tag VARCHAR(32) NOT NULL DEFAULT '出售帖' COMMENT '帖子标签' AFTER play_type");
        for (String ddl : PERF_INDEXES) {
            executeIdempotent(ddl);
        }
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM domains WHERE host = 'entry.127.0.0.1'", Integer.class);
            if (n == null || n == 0) {
                jdbcTemplate.execute(
                        "INSERT INTO domains (id, tenant_id, host, is_primary, role, status) "
                                + "VALUES (2005, 1001, 'entry.127.0.0.1', 0, 'ENTRY', 1)");
            }
        } catch (Exception e) {
            log.debug("Seed entry domain skip: {}", e.getMessage());
        }
    }

    private void executeIdempotent(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("Schema migrate OK: {}", sql.substring(0, Math.min(sql.length(), 60)));
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
                        || lower.contains("already exists")) {
                    return true;
                }
            }
            e = e.getCause();
        }
        return false;
    }
}
