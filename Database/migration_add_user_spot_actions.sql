USE tourist_system;

CREATE TABLE IF NOT EXISTS user_spot_actions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户景点行为状态ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type ENUM('poi') NOT NULL DEFAULT 'poi' COMMENT '行为对象类型',
  target_id BIGINT NOT NULL COMMENT '行为对象ID',
  action_type ENUM('want', 'visited', 'dislike') NOT NULL COMMENT '行为类型: 想去/去过/不感兴趣',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效: 1有效 0取消',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_spot_actions_user_target_action (user_id, target_type, target_id, action_type),
  KEY idx_user_spot_actions_target (target_type, target_id),
  KEY idx_user_spot_actions_user_action (user_id, action_type, active, updated_at),
  CONSTRAINT fk_user_spot_actions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户景点想去/去过状态表';
