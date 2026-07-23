-- M6 draw tables
USE liuhecai;

CREATE TABLE IF NOT EXISTS draw_results_global (
    id              BIGINT PRIMARY KEY,
    lottery_type    VARCHAR(32)  NOT NULL COMMENT 'MACAU_NEW/HK/MACAU_OLD',
    issue_no        VARCHAR(32)  NOT NULL,
    draw_time       DATETIME     NOT NULL,
    numbers_json    VARCHAR(128) NOT NULL COMMENT '六码 JSON 数组',
    special_number  VARCHAR(8)   NOT NULL,
    zodiac_json     VARCHAR(256) NULL COMMENT '含特码生肖',
    source          VARCHAR(64)  NOT NULL DEFAULT 'mock',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_draw_global (lottery_type, issue_no),
    KEY idx_draw_global_type_time (lottery_type, draw_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS draw_overrides (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    lottery_type    VARCHAR(32)  NOT NULL,
    issue_no        VARCHAR(32)  NOT NULL,
    draw_time       DATETIME     NOT NULL,
    numbers_json    VARCHAR(128) NOT NULL,
    special_number  VARCHAR(8)   NOT NULL,
    zodiac_json     VARCHAR(256) NULL,
    note            VARCHAR(256) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_draw_override (tenant_id, lottery_type, issue_no),
    KEY idx_draw_override_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
