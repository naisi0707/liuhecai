-- M15 performance indexes (also applied idempotently by SchemaMigrateRunner)
USE liuhecai;

-- 用户注册趋势 / 按租户时间范围
ALTER TABLE users ADD INDEX idx_users_tenant_created (tenant_id, created_at);
ALTER TABLE users ADD INDEX idx_users_created (created_at);

-- 购帖趋势
ALTER TABLE topic_orders ADD INDEX idx_topic_orders_tenant_created (tenant_id, created_at);
ALTER TABLE topic_orders ADD INDEX idx_topic_orders_created (created_at);

-- 充值趋势（status + handled_at）
ALTER TABLE recharge_requests ADD INDEX idx_recharge_tenant_status_handled (tenant_id, status, handled_at);

-- 公开专题列表：status + created_at
ALTER TABLE topics ADD INDEX idx_topics_tenant_status_created (tenant_id, status, created_at);
ALTER TABLE topics ADD INDEX idx_topics_updated (updated_at);
