-- Seed ENTRY lines for 157465.com (run after schema-m15)
SET @entry_id = (SELECT id FROM domains WHERE host = '157465.com' AND role = 'ENTRY' LIMIT 1);
SET @entry_www = (SELECT id FROM domains WHERE host = 'www.157465.com' AND role = 'ENTRY' LIMIT 1);

-- 主入口
DELETE FROM entry_lines WHERE entry_domain_id = @entry_id;
INSERT INTO entry_lines (id, entry_domain_id, sort_order, label, color, target_tenant_id, status) VALUES
(5101, @entry_id, 1, '电信临时线路', '#c62828', 1001, 1),
(5102, @entry_id, 2, '移动临时线路', '#1565c0', 1003, 1),
(5103, @entry_id, 3, '联通临时线路', '#2e7d32', 1004, 1),
(5104, @entry_id, 4, '广电临时线路', '#6a1b9a', 1002, 1),
(5105, @entry_id, 5, '澳门直达专线', '#ef6c00', 1005, 1);

-- www 入口（若存在）同步一份
DELETE FROM entry_lines WHERE entry_domain_id = @entry_www AND @entry_www IS NOT NULL;
INSERT INTO entry_lines (id, entry_domain_id, sort_order, label, color, target_tenant_id, status)
SELECT id + 10, @entry_www, sort_order, label, color, target_tenant_id, status
FROM entry_lines
WHERE entry_domain_id = @entry_id AND @entry_www IS NOT NULL;
