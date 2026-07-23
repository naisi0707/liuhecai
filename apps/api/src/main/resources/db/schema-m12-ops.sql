-- M12 Ops: token version + audit logs
USE liuhecai;

ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE agent_accounts ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE super_admins ADD COLUMN token_version INT NOT NULL DEFAULT 0;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
