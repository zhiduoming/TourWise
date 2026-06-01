-- 推荐负反馈：支持“不感兴趣”，并把相似标签降权
-- 执行方式：在 IDEA Database Console 中运行本文件。

USE tourist_system;

ALTER TABLE user_spot_actions
  MODIFY COLUMN action_type ENUM('want', 'visited', 'dislike') NOT NULL COMMENT '行为类型: 想去/去过/不感兴趣';

ALTER TABLE user_preferences
  MODIFY COLUMN source ENUM('manual', 'browse', 'favorite', 'want', 'visited', 'dislike', 'like', 'comment', 'rating', 'circle')
  NOT NULL DEFAULT 'manual'
  COMMENT '偏好来源';
