USE tourist_system;

CREATE TABLE IF NOT EXISTS itinerary_plans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程计划ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(100) NOT NULL COMMENT '行程标题',
  city VARCHAR(50) NOT NULL COMMENT '目的城市',
  duration VARCHAR(30) NOT NULL COMMENT '游玩时长',
  pace VARCHAR(30) NOT NULL COMMENT '游玩节奏',
  total_days INT NOT NULL DEFAULT 1 COMMENT '总天数',
  spot_count INT NOT NULL DEFAULT 0 COMMENT '景点数量',
  summary VARCHAR(500) DEFAULT NULL COMMENT '行程摘要',
  preferences_json JSON DEFAULT NULL COMMENT '偏好JSON',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_itinerary_plans_user_time (user_id, status, updated_at),
  CONSTRAINT fk_itinerary_plans_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户旅行计划表';

CREATE TABLE IF NOT EXISTS itinerary_plan_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程节点ID',
  plan_id BIGINT NOT NULL COMMENT '行程计划ID',
  day_no INT NOT NULL COMMENT '第几天',
  order_no INT NOT NULL COMMENT '当天排序',
  item_type ENUM('spot', 'food') NOT NULL DEFAULT 'spot' COMMENT '节点类型',
  time_slot VARCHAR(30) DEFAULT NULL COMMENT '时间段',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID，景点为POI ID，美食为foods.id',
  spot_id BIGINT DEFAULT NULL COMMENT '关联景点ID',
  place_group_id BIGINT DEFAULT NULL COMMENT '关联空间分组ID',
  name VARCHAR(100) NOT NULL COMMENT '节点名称',
  description TEXT COMMENT '节点说明',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  image_url VARCHAR(255) DEFAULT NULL COMMENT '图片',
  rating DECIMAL(3, 1) DEFAULT NULL COMMENT '评分',
  hotness INT DEFAULT NULL COMMENT '热度',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  recommend_reason VARCHAR(255) DEFAULT NULL COMMENT '推荐理由',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_itinerary_plan_items_plan (plan_id, day_no, order_no),
  CONSTRAINT fk_itinerary_items_plan FOREIGN KEY (plan_id) REFERENCES itinerary_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户旅行计划节点表';
