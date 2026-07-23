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

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String ddl : ADD_TOKEN_VERSION) {
            executeIdempotent(ddl);
        }
        executeIdempotent(CREATE_OP_AUDIT_LOGS);
    }

    private void executeIdempotent(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("Schema migrate OK: {}", sql.substring(0, Math.min(sql.length(), 60)));
        } catch (Exception e) {
            if (isDuplicateColumn(e)) {
                log.debug("Schema migrate skip (duplicate column): {}", e.getMessage());
                return;
            }
            throw e;
        }
    }

    private static boolean isDuplicateColumn(Throwable e) {
        while (e != null) {
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate column")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }
}
