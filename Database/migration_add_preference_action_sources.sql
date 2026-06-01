-- 让推荐系统可以从“想去/去过”行为中沉淀兴趣标签
-- 执行方式：在 IDEA Database Console 中运行本文件。

USE tourist_system;

ALTER TABLE user_preferences
  MODIFY COLUMN source ENUM('manual', 'browse', 'favorite', 'want', 'visited', 'dislike', 'like', 'comment', 'rating', 'circle')
  NOT NULL DEFAULT 'manual'
  COMMENT '偏好来源';
