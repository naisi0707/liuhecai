-- M8 recharge_requests
USE liuhecai;

CREATE TABLE IF NOT EXISTS recharge_requests (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    amount          INT          NOT NULL,
    pay_channel     VARCHAR(64)  NULL COMMENT '转账渠道说明',
    remark          VARCHAR(256) NULL COMMENT '用户备注/凭证说明',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1已通过 2已拒绝',
    reject_reason   VARCHAR(256) NULL,
    handled_by      BIGINT       NULL,
    handled_at      DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recharge_tenant_status (tenant_id, status),
    KEY idx_recharge_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
