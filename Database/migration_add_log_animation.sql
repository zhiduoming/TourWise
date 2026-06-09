-- 为 travel_logs 增加 AIGC 旅游动画相关字段
-- 数据结构课设要求 (4)-⑨：使用 AIGC 算法根据日记照片生成旅游动画
-- 调用智谱 CogVideoX 图生视频 API，提交后异步生成、轮询回写。

USE tourist_system;

ALTER TABLE travel_logs
  ADD COLUMN animation_url VARCHAR(500) NULL COMMENT 'AIGC 动画视频 URL' AFTER value_rating,
  ADD COLUMN animation_cover_url VARCHAR(500) NULL COMMENT 'AIGC 动画封面图 URL' AFTER animation_url,
  ADD COLUMN animation_status VARCHAR(20) NULL COMMENT 'pending/processing/success/failed' AFTER animation_cover_url,
  ADD COLUMN animation_task_id VARCHAR(100) NULL COMMENT '智谱视频生成任务 ID' AFTER animation_status,
  ADD COLUMN animation_error VARCHAR(500) NULL COMMENT '失败时的错误描述' AFTER animation_task_id,
  ADD COLUMN animation_updated_at DATETIME NULL COMMENT '动画状态最后更新时间' AFTER animation_error,
  ADD KEY idx_travel_logs_animation_task (animation_task_id);
