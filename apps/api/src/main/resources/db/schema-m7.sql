-- M7 topics / orders / coin_logs + user version for optimistic lock
USE liuhecai;

-- version 列：若 users 尚无该列，执行：
-- ALTER TABLE users ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER coin_balance;

CREATE TABLE IF NOT EXISTS topics (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    title           VARCHAR(128) NOT NULL,
    lottery_type    VARCHAR(32)  NOT NULL,
    issue_no        VARCHAR(32)  NOT NULL,
    play_type       VARCHAR(64)  NOT NULL DEFAULT '综合',
    price           INT          NOT NULL DEFAULT 0,
    content         TEXT         NOT NULL,
    preview_content MEDIUMTEXT   NULL COMMENT '往期成绩等公开预览 HTML',
    view_count      INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1已通过 2拒绝 3下架',
    created_by      BIGINT       NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_topics_tenant_status (tenant_id, status),
    KEY idx_topics_tenant_issue (tenant_id, issue_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS topic_orders (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    topic_id        BIGINT       NOT NULL,
    price           INT          NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_topic_order (tenant_id, user_id, topic_id),
    KEY idx_topic_orders_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coin_logs (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    change_amount   INT          NOT NULL,
    balance_after   INT          NOT NULL,
    biz_type        VARCHAR(32)  NOT NULL COMMENT 'PURCHASE/GRANT/RECHARGE',
    biz_id          VARCHAR(64)  NULL,
    remark          VARCHAR(256) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coin_logs_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
