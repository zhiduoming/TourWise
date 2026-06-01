-- 管理员角色与路线底图表迁移
-- 用途：
-- 1. 给 users 增加 role 字段，用于区分普通用户和管理员。
-- 2. 创建 place_group_maps，保存每个景点/校区对应的路线底图。
-- 3. 将演示账号 id=1 设置为管理员，方便本地答辩演示。

USE tourist_system;

SET @has_role := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'role'
);

SET @sql := IF(
  @has_role = 0,
  'ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''user'' COMMENT ''角色: user普通用户 admin管理员'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS place_group_maps (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地图图片ID',
  place_group_id BIGINT NOT NULL COMMENT '空间分组ID',
  image_url VARCHAR(255) NOT NULL COMMENT '地图图片URL',
  original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  map_width INT DEFAULT NULL COMMENT '地图图片像素宽度',
  map_height INT DEFAULT NULL COMMENT '地图图片像素高度',
  uploaded_by BIGINT DEFAULT NULL COMMENT '上传管理员ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_place_group_maps_group (place_group_id),
  KEY idx_place_group_maps_uploaded_by (uploaded_by),
  CONSTRAINT fk_place_group_maps_group FOREIGN KEY (place_group_id) REFERENCES place_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_place_group_maps_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点/校区路线底图表';

UPDATE users
SET role = 'admin'
WHERE id = 1;
