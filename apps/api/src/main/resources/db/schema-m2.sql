-- M2 schema + seed (run against local MySQL)
CREATE DATABASE IF NOT EXISTS liuhecai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE liuhecai;

CREATE TABLE IF NOT EXISTS tenants (
    id            BIGINT PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    theme_json    JSON         NULL,
    kefu_wechat   VARCHAR(64)  NULL,
    kefu_qq       VARCHAR(32)  NULL,
    announcement  VARCHAR(512) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS domains (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    host        VARCHAR(128) NOT NULL,
    is_primary  TINYINT      NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_domains_host (host),
    KEY idx_domains_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用于验证 TenantLine 隔离的业务示例表
CREATE TABLE IF NOT EXISTS demo_notes (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    title       VARCHAR(128) NOT NULL,
    content     VARCHAR(512) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_demo_notes_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM demo_notes;
DELETE FROM domains;
DELETE FROM tenants;

INSERT INTO tenants (id, name, status, theme_json, kefu_wechat, announcement) VALUES
(1001, '刘伯温论坛', 1, JSON_OBJECT('primaryColor', '#c62828', 'fontFamily', 'Microsoft YaHei'), 'lbw_kefu', '欢迎来到刘伯温论坛（演示站 A）'),
(1002, '至尊无上论坛', 1, JSON_OBJECT('primaryColor', '#1565c0', 'fontFamily', 'Microsoft YaHei'), 'zzws_kefu', '欢迎来到至尊无上（演示站 B）');

INSERT INTO domains (id, tenant_id, host, is_primary, status) VALUES
(2001, 1001, 'lbw.local', 1, 1),
(2002, 1002, 'zzws.local', 1, 1),
(2003, 1001, 'localhost', 0, 1),
(2004, 1001, '127.0.0.1', 0, 1);

INSERT INTO demo_notes (id, tenant_id, title, content) VALUES
(3001, 1001, 'A站内部笔记', '仅租户1001可见'),
(3002, 1002, 'B站内部笔记', '仅租户1002可见');
