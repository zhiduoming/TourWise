-- 给景点日志增加可选的多维评分字段。
-- 不填写评分时字段保持 NULL，不参与景点平均分计算。

SET @has_scenery_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'travel_logs'
    AND COLUMN_NAME = 'scenery_rating'
);
SET @sql := IF(
  @has_scenery_rating = 0,
  'ALTER TABLE travel_logs ADD COLUMN scenery_rating DECIMAL(3,1) DEFAULT NULL COMMENT ''景观体验评分'' AFTER rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_facility_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'travel_logs'
    AND COLUMN_NAME = 'facility_rating'
);
SET @sql := IF(
  @has_facility_rating = 0,
  'ALTER TABLE travel_logs ADD COLUMN facility_rating DECIMAL(3,1) DEFAULT NULL COMMENT ''设施完善评分'' AFTER scenery_rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_service_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'travel_logs'
    AND COLUMN_NAME = 'service_rating'
);
SET @sql := IF(
  @has_service_rating = 0,
  'ALTER TABLE travel_logs ADD COLUMN service_rating DECIMAL(3,1) DEFAULT NULL COMMENT ''服务体验评分'' AFTER facility_rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_traffic_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'travel_logs'
    AND COLUMN_NAME = 'traffic_rating'
);
SET @sql := IF(
  @has_traffic_rating = 0,
  'ALTER TABLE travel_logs ADD COLUMN traffic_rating DECIMAL(3,1) DEFAULT NULL COMMENT ''交通便利评分'' AFTER service_rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_value_rating := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'travel_logs'
    AND COLUMN_NAME = 'value_rating'
);
SET @sql := IF(
  @has_value_rating = 0,
  'ALTER TABLE travel_logs ADD COLUMN value_rating DECIMAL(3,1) DEFAULT NULL COMMENT ''性价比评分'' AFTER traffic_rating',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
