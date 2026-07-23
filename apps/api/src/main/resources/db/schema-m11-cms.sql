-- M11 CMS: site menus + pages (tenant scoped)
USE liuhecai;

CREATE TABLE IF NOT EXISTS site_menus (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(32)  NOT NULL COMMENT 'home|rules|recharge|kefu|login|register',
    title       VARCHAR(64)  NOT NULL,
    path        VARCHAR(128) NOT NULL,
    sort_no     INT          NOT NULL DEFAULT 0,
    visible     TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_menus_tenant_code (tenant_id, code),
    KEY idx_site_menus_tenant_sort (tenant_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_pages (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    page_key      VARCHAR(32)  NOT NULL COMMENT 'home|rules|recharge|kefu',
    title         VARCHAR(128) NOT NULL,
    content_json  MEDIUMTEXT   NOT NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_pages_tenant_key (tenant_id, page_key),
    KEY idx_site_pages_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
