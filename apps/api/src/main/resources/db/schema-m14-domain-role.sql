-- M14: domains.role — ENTRY 入口伪装 / FORUM 论坛（默认）
USE liuhecai;

ALTER TABLE domains
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'FORUM' COMMENT 'ENTRY入口伪装 FORUM论坛' AFTER is_primary;

-- 演示：入口域（与 127.0.0.1 论坛域配对）
INSERT INTO domains (id, tenant_id, host, is_primary, role, status)
SELECT 2005, 1001, 'entry.127.0.0.1', 0, 'ENTRY', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM domains WHERE host = 'entry.127.0.0.1');
