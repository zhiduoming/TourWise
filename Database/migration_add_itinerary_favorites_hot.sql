USE tourist_system;

ALTER TABLE itinerary_plans
  ADD COLUMN source_plan_id BIGINT DEFAULT NULL COMMENT '复制来源行程ID' AFTER user_id,
  ADD COLUMN copy_count INT NOT NULL DEFAULT 0 COMMENT '被复制次数' AFTER spot_count,
  ADD COLUMN favorite_count INT NOT NULL DEFAULT 0 COMMENT '被收藏次数' AFTER copy_count,
  ADD KEY idx_itinerary_plans_source (source_plan_id),
  ADD KEY idx_itinerary_plans_hot (status, favorite_count, copy_count, updated_at);

ALTER TABLE itinerary_plans
  ADD CONSTRAINT fk_itinerary_plans_source
  FOREIGN KEY (source_plan_id) REFERENCES itinerary_plans(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS itinerary_plan_favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程收藏ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  plan_id BIGINT NOT NULL COMMENT '行程计划ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_itinerary_favorite_user_plan (user_id, plan_id),
  KEY idx_itinerary_favorites_plan (plan_id),
  CONSTRAINT fk_itinerary_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_itinerary_favorites_plan FOREIGN KEY (plan_id) REFERENCES itinerary_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏行程表';
