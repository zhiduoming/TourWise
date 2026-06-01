USE tourist_system;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  actor_user_id BIGINT DEFAULT NULL COMMENT '触发用户ID',
  type VARCHAR(30) NOT NULL COMMENT '通知类型: log_like/log_comment/itinerary_copy',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
  target_type VARCHAR(30) DEFAULT NULL COMMENT '目标类型',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID',
  link_url VARCHAR(255) DEFAULT NULL COMMENT '前端跳转地址',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_notifications_user_read_time (user_id, is_read, created_at),
  KEY idx_notifications_target (target_type, target_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';
