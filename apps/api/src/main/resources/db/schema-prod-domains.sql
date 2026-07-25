-- Production tenants + domains (run after schema-m2 / base schemas)
-- Agents are NOT inserted here: AuthSeedRunner ensures one primary agent per tenant
-- (agent_a/b/ssz/zcb/rhfg) and DB FKs require tenants.primary_agent_id.
USE liuhecai;

-- Keep seed tenants 1001/1002; add remaining brands
INSERT INTO tenants (id, name, status, theme_json, kefu_wechat, announcement) VALUES
(1001, '刘伯温论坛', 1, JSON_OBJECT('primaryColor', '#c62828', 'fontFamily', 'Microsoft YaHei'), 'lbw_kefu', '欢迎来到刘伯温论坛'),
(1002, '至尊无上论坛', 1, JSON_OBJECT('primaryColor', '#1565c0', 'fontFamily', 'Microsoft YaHei'), 'zzws_kefu', '欢迎来到至尊无上'),
(1003, '神算子论坛', 1, JSON_OBJECT('primaryColor', '#2e7d32', 'fontFamily', 'Microsoft YaHei'), 'ssz_kefu', '欢迎来到神算子'),
(1004, '招财宝论坛', 1, JSON_OBJECT('primaryColor', '#ef6c00', 'fontFamily', 'Microsoft YaHei'), 'zcb_kefu', '欢迎来到招财宝'),
(1005, '荣华富贵论坛', 1, JSON_OBJECT('primaryColor', '#6a1b9a', 'fontFamily', 'Microsoft YaHei'), 'rhfg_kefu', '欢迎来到荣华富贵')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  theme_json = VALUES(theme_json),
  announcement = VALUES(announcement);

-- Replace demo local hosts with production hosts
DELETE FROM domains WHERE host IN (
  'lbw.local', 'zzws.local', 'localhost', '127.0.0.1', 'entry.127.0.0.1',
  '157456.com', 'www.157456.com', '157465.com', 'www.157465.com',
  '585520.xyz', '785412.xyz', '658951.xyz', '152687.xyz', '746528.xyz'
);

INSERT INTO domains (id, tenant_id, host, is_primary, role, status) VALUES
(2101, 1001, '585520.xyz', 1, 'FORUM', 1),
(2102, 1001, '157465.com', 0, 'ENTRY', 1),
(2103, 1001, 'www.157465.com', 0, 'ENTRY', 1),
(2104, 1002, '152687.xyz', 1, 'FORUM', 1),
(2105, 1003, '785412.xyz', 1, 'FORUM', 1),
(2106, 1004, '658951.xyz', 1, 'FORUM', 1),
(2107, 1005, '746528.xyz', 1, 'FORUM', 1);
