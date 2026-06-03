-- 为 foods 表补齐 AI 简介、经纬度字段，并新增按 POI 名称的索引便于聚合
-- AI 简介复用 DeepSeek，首访时生成并缓存，支持点击重新生成覆盖
-- 经纬度用于美食详情右栏渲染高德地图（具体店铺坐标，非校区中心）

USE tourist_system;

ALTER TABLE foods
  ADD COLUMN ai_summary TEXT NULL COMMENT 'AI 生成的美食介绍，首访生成并缓存' AFTER description,
  ADD COLUMN longitude DECIMAL(10, 6) NULL COMMENT '经度(高德 GCJ-02)' AFTER address,
  ADD COLUMN latitude  DECIMAL(10, 6) NULL COMMENT '纬度(高德 GCJ-02)' AFTER longitude;

CREATE INDEX idx_foods_status_poi ON foods (status, poi_id, rating DESC);
