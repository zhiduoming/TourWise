-- 为 travel_logs 表增加 food_id 列，支持美食日志
-- 此前 travel_logs 只有 poi_id（外键到 pois），美食日志只能曲折挂到所属 POI；
-- 新增 food_id 直接关联 foods，让美食详情页可以筛选该店铺下的日志，并支持发布美食日志。

USE tourist_system;

ALTER TABLE travel_logs
  ADD COLUMN food_id BIGINT NULL COMMENT '关联美食ID' AFTER poi_id,
  ADD KEY idx_travel_logs_food (food_id),
  ADD CONSTRAINT fk_travel_logs_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE SET NULL;
