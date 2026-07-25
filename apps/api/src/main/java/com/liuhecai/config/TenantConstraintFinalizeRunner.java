package com.liuhecai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * After AuthSeedRunner backfill: enforce tenants.primary_agent_id NOT NULL + FK.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class TenantConstraintFinalizeRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer missing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE primary_agent_id IS NULL", Integer.class);
        if (missing != null && missing > 0) {
            log.warn("Skip primary_agent_id NOT NULL: {} tenants still missing primary agent", missing);
            return;
        }
        executeQuiet(
                "ALTER TABLE tenants MODIFY primary_agent_id BIGINT NOT NULL COMMENT '主代理id'");
        executeQuiet(
                "ALTER TABLE tenants ADD CONSTRAINT fk_tenant_primary_agent "
                        + "FOREIGN KEY (primary_agent_id) REFERENCES agent_accounts(id) "
                        + "ON DELETE RESTRICT ON UPDATE CASCADE");
        log.info("Tenant primary_agent_id NOT NULL + FK enforced");
    }

    private void executeQuiet(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Constraint OK: {}", sql.substring(0, Math.min(sql.length(), 80)));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("already exists") || msg.contains("errno: 121")) {
                log.debug("Constraint skip: {}", e.getMessage());
                return;
            }
            // "same as previous" / no change for MODIFY
            if (msg.contains("nothing is changed") || msg.contains("identical")) {
                return;
            }
            log.warn("Constraint apply warn: {} — {}", sql.substring(0, Math.min(40, sql.length())), e.getMessage());
        }
    }
}
