-- M15: entry_lines — per ENTRY domain camouflage route buttons
CREATE TABLE IF NOT EXISTS entry_lines (
    id                BIGINT PRIMARY KEY,
    entry_domain_id   BIGINT       NOT NULL COMMENT 'domains.id role=ENTRY',
    sort_order        INT          NOT NULL DEFAULT 0,
    label             VARCHAR(64)  NOT NULL,
    color             VARCHAR(16)  NOT NULL DEFAULT '#c62828',
    target_tenant_id  BIGINT       NOT NULL,
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_entry_lines_domain (entry_domain_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
