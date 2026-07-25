-- M17: tenant primary agent + FKs (apply via SchemaMigrateRunner; kept for reference)
-- MySQL 5.7 compatible: primary_key is app-maintained (1 for primary, NULL otherwise)

ALTER TABLE agent_accounts
  ADD COLUMN is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '1主代理' AFTER enabled;

ALTER TABLE agent_accounts
  ADD COLUMN primary_key TINYINT NULL COMMENT '主代理唯一槽:1/NULL' AFTER is_primary;

ALTER TABLE agent_accounts
  ADD UNIQUE KEY uk_agent_tenant_primary (tenant_id, primary_key);

ALTER TABLE tenants
  ADD COLUMN primary_agent_id BIGINT NULL COMMENT '主代理 agent_accounts.id' AFTER status;

-- Backfill then:
-- ALTER TABLE tenants MODIFY primary_agent_id BIGINT NOT NULL;
-- ADD CONSTRAINT fk_agent_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
-- ADD CONSTRAINT fk_domain_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
-- ADD CONSTRAINT fk_tenant_primary_agent FOREIGN KEY (primary_agent_id) REFERENCES agent_accounts(id)
