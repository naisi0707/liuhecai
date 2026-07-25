-- M16: topics.tag — 帖子标签（出售帖/高手帖/普通帖/推荐帖等，可自定义）
USE liuhecai;

ALTER TABLE topics
    ADD COLUMN tag VARCHAR(32) NOT NULL DEFAULT '出售帖' COMMENT '帖子标签' AFTER play_type;
