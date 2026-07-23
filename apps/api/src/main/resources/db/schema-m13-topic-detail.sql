-- M13: 帖子详情 — 往期预览 + 浏览量（幂等：列已存在时忽略报错即可）
USE liuhecai;

ALTER TABLE topics
    ADD COLUMN preview_content MEDIUMTEXT NULL COMMENT '往期成绩等公开预览 HTML' AFTER content;

ALTER TABLE topics
    ADD COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量' AFTER preview_content;
