-- 景点 / 内部 POI / 路线节点三层模型重构
--
-- 目标：
-- 1. spots：景点主表，存浙江大学、上海外滩、北邮沙河校区这种“大地点”。
-- 2. pois：景点内部 POI，存图书馆、食堂、教学楼、广场等用户可访问点位。
-- 3. route_nodes：路线算法节点，存路口、建筑入口、可通行节点。
-- 4. route_node_edges：路线算法边，连接 route_nodes。
--
-- 兼容策略：
-- - 不删除旧 pois / route_edges。
-- - 给旧 pois 增加 spot_id 和 poi_role，先让数据语义变清楚。
-- - 从旧 route_edges 迁移出 route_nodes / route_node_edges，后端可后续切换。

USE tourist_system;

SET @has_poi_spot_id := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'spot_id'
);

SET @sql := IF(
  @has_poi_spot_id = 0,
  'ALTER TABLE pois ADD COLUMN spot_id BIGINT DEFAULT NULL COMMENT ''所属景点ID，重构后指向 spots.id'' AFTER id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_poi_role := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'poi_role'
);

SET @sql := IF(
  @has_poi_role = 0,
  'ALTER TABLE pois ADD COLUMN poi_role VARCHAR(30) NOT NULL DEFAULT ''internal'' COMMENT ''POI角色: internal内部点位 spot_legacy旧景点行 route_legacy旧路线节点'' AFTER spot_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS spots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '景点ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级景点ID，如高校本部 -> 校区',
  legacy_place_group_id BIGINT DEFAULT NULL COMMENT '来源 place_groups.id，兼容旧空间分组',
  legacy_poi_id BIGINT DEFAULT NULL COMMENT '来源 pois.id，兼容旧景点型 POI',
  representative_poi_id BIGINT DEFAULT NULL COMMENT '兼容旧前端详情页的代表 POI ID',
  category_id BIGINT DEFAULT NULL COMMENT '景点分类ID',
  name VARCHAR(100) NOT NULL COMMENT '景点名称',
  short_name VARCHAR(50) DEFAULT NULL COMMENT '简称',
  spot_type VARCHAR(50) NOT NULL DEFAULT 'other' COMMENT '景点类型: university/campus/scenic/museum/park/landmark/business/other',
  province VARCHAR(50) DEFAULT NULL COMMENT '省份/直辖市/自治区',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  district VARCHAR(50) DEFAULT NULL COMMENT '区县',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  description TEXT COMMENT '景点介绍',
  cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心纬度',
  rating DECIMAL(3, 1) NOT NULL DEFAULT 5.0 COMMENT '评分',
  hotness INT NOT NULL DEFAULT 0 COMMENT '热度',
  visit_count INT NOT NULL DEFAULT 0 COMMENT '访问量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_spots_legacy_place_group (legacy_place_group_id),
  UNIQUE KEY uk_spots_legacy_poi (legacy_poi_id),
  KEY idx_spots_parent (parent_id),
  KEY idx_spots_category (category_id),
  KEY idx_spots_region (province, city),
  KEY idx_spots_type_hotness (spot_type, hotness),
  FULLTEXT KEY ft_spots_name_desc (name, description),
  CONSTRAINT fk_spots_category FOREIGN KEY (category_id) REFERENCES poi_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点主表，存景区/高校/校区/商圈等大地点';

CREATE TABLE IF NOT EXISTS spot_tag_relations (
  spot_id BIGINT NOT NULL COMMENT '景点ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (spot_id, tag_id),
  KEY idx_spot_tags_tag (tag_id),
  CONSTRAINT fk_spot_tags_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE,
  CONSTRAINT fk_spot_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点标签关联表';

CREATE TABLE IF NOT EXISTS route_nodes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线节点ID',
  spot_id BIGINT NOT NULL COMMENT '所属景点ID',
  legacy_poi_id BIGINT DEFAULT NULL COMMENT '来源 pois.id，兼容旧路线图',
  name VARCHAR(100) NOT NULL COMMENT '节点名称',
  node_type VARCHAR(30) NOT NULL DEFAULT 'intersection' COMMENT '节点类型: poi/gate/intersection/entrance',
  visible TINYINT NOT NULL DEFAULT 0 COMMENT '是否展示给用户选择',
  map_x INT DEFAULT NULL COMMENT '平面图X坐标',
  map_y INT DEFAULT NULL COMMENT '平面图Y坐标',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_route_nodes_legacy_poi (legacy_poi_id),
  KEY idx_route_nodes_spot_visible (spot_id, visible),
  CONSTRAINT fk_route_nodes_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线算法节点表，路口/入口/可通行点单独存放';

CREATE TABLE IF NOT EXISTS route_node_edges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线边ID',
  spot_id BIGINT NOT NULL COMMENT '所属景点ID',
  from_node_id BIGINT NOT NULL COMMENT '起点路线节点ID',
  to_node_id BIGINT NOT NULL COMMENT '终点路线节点ID',
  distance_m INT NOT NULL COMMENT '距离，单位米',
  duration_min INT NOT NULL COMMENT '预计耗时，单位分钟',
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk' COMMENT '通行方式',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '拥挤系数',
  is_indoor TINYINT NOT NULL DEFAULT 0 COMMENT '是否室内路线',
  description VARCHAR(255) DEFAULT NULL COMMENT '路段说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_route_node_edges_unique (from_node_id, to_node_id, transport_type),
  KEY idx_route_node_edges_spot (spot_id),
  KEY idx_route_node_edges_from (from_node_id),
  KEY idx_route_node_edges_to (to_node_id),
  CONSTRAINT fk_route_node_edges_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_node_edges_from FOREIGN KEY (from_node_id) REFERENCES route_nodes(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_node_edges_to FOREIGN KEY (to_node_id) REFERENCES route_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线算法边表，连接 route_nodes';

-- 从 place_groups 生成高校/校区/公园/商圈等景点主表数据。
INSERT INTO spots
  (legacy_place_group_id, representative_poi_id, category_id, name, short_name, spot_type,
   province, city, district, address, description, longitude, latitude, rating, hotness, visit_count, status)
SELECT
  pg.id AS legacy_place_group_id,
  (
    SELECT p.id
    FROM pois p
    WHERE p.place_group_id = pg.id
      AND p.area_code = 'campus_summary'
    ORDER BY p.id
    LIMIT 1
  ) AS representative_poi_id,
  CASE
    WHEN pg.group_type IN ('university', 'campus') THEN (SELECT id FROM poi_categories WHERE code = 'university' LIMIT 1)
    WHEN pg.group_type = 'park' THEN (SELECT id FROM poi_categories WHERE code = 'natural' LIMIT 1)
    WHEN pg.group_type = 'business_area' THEN (SELECT id FROM poi_categories WHERE code = 'commercial' LIMIT 1)
    WHEN pg.group_type = 'museum_area' THEN (SELECT id FROM poi_categories WHERE code = 'museum' LIMIT 1)
    ELSE (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1)
  END AS category_id,
  pg.name,
  pg.short_name,
  CASE
    WHEN pg.group_type = 'university' THEN 'university'
    WHEN pg.group_type = 'campus' THEN 'campus'
    WHEN pg.group_type = 'park' THEN 'park'
    WHEN pg.group_type = 'business_area' THEN 'business'
    WHEN pg.group_type = 'museum_area' THEN 'museum'
    WHEN pg.group_type = 'scenic_area' THEN 'scenic'
    ELSE 'other'
  END AS spot_type,
  CASE WHEN pg.city IN ('北京', '上海', '天津', '重庆') THEN pg.city ELSE pg.city END AS province,
  pg.city,
  pg.district,
  pg.address,
  pg.description,
  pg.longitude,
  pg.latitude,
  COALESCE((
    SELECT p.rating
    FROM pois p
    WHERE p.place_group_id = pg.id
      AND p.area_code = 'campus_summary'
    ORDER BY p.id
    LIMIT 1
  ), 4.6) AS rating,
  COALESCE((
    SELECT p.hotness
    FROM pois p
    WHERE p.place_group_id = pg.id
      AND p.area_code = 'campus_summary'
    ORDER BY p.id
    LIMIT 1
  ), pg.sort_order * 100) AS hotness,
  COALESCE((
    SELECT p.visit_count
    FROM pois p
    WHERE p.place_group_id = pg.id
      AND p.area_code = 'campus_summary'
    ORDER BY p.id
    LIMIT 1
  ), pg.sort_order * 100) AS visit_count,
  1
FROM place_groups pg
WHERE pg.id <> 100
ON DUPLICATE KEY UPDATE
  representative_poi_id = VALUES(representative_poi_id),
  category_id = VALUES(category_id),
  name = VALUES(name),
  short_name = VALUES(short_name),
  spot_type = VALUES(spot_type),
  province = VALUES(province),
  city = VALUES(city),
  district = VALUES(district),
  address = VALUES(address),
  description = VALUES(description),
  longitude = VALUES(longitude),
  latitude = VALUES(latitude),
  rating = VALUES(rating),
  hotness = VALUES(hotness),
  visit_count = VALUES(visit_count),
  status = VALUES(status);

-- 维护高校 -> 校区父子关系。
UPDATE spots child
JOIN place_groups pg ON pg.id = child.legacy_place_group_id
JOIN spots parent ON parent.legacy_place_group_id = pg.parent_id
SET child.parent_id = parent.id
WHERE pg.parent_id IS NOT NULL;

-- 从全国热门景点/高校占位 POI 生成真正的 spots。
INSERT INTO spots
  (legacy_poi_id, representative_poi_id, category_id, name, short_name, spot_type,
   province, city, district, address, description, cover_image, longitude, latitude,
   rating, hotness, visit_count, status)
SELECT
  p.id AS legacy_poi_id,
  p.id AS representative_poi_id,
  p.category_id,
  p.name,
  p.location_text AS short_name,
  CASE
    WHEN c.code = 'university' THEN 'university'
    WHEN c.code = 'museum' THEN 'museum'
    WHEN c.code IN ('natural', 'waterfront') THEN 'park'
    WHEN c.code IN ('landmark', 'commercial') THEN 'landmark'
    ELSE 'scenic'
  END AS spot_type,
  p.province,
  p.city,
  NULL AS district,
  p.address,
  p.description,
  p.image_url,
  p.longitude,
  p.latitude,
  p.rating,
  p.hotness,
  p.visit_count,
  p.status
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
WHERE p.place_group_id = 100
ON DUPLICATE KEY UPDATE
  representative_poi_id = VALUES(representative_poi_id),
  category_id = VALUES(category_id),
  name = VALUES(name),
  short_name = VALUES(short_name),
  spot_type = VALUES(spot_type),
  province = VALUES(province),
  city = VALUES(city),
  address = VALUES(address),
  description = VALUES(description),
  cover_image = VALUES(cover_image),
  longitude = VALUES(longitude),
  latitude = VALUES(latitude),
  rating = VALUES(rating),
  hotness = VALUES(hotness),
  visit_count = VALUES(visit_count),
  status = VALUES(status);

-- 给旧 POI 标清角色，避免“浙江大学”和“甲子钟路口”在语义上继续混成一类。
UPDATE pois
SET poi_role = CASE
    WHEN area_code = 'route' THEN 'route_legacy'
    WHEN area_code = 'campus_summary' OR place_group_id = 100 THEN 'spot_legacy'
    ELSE 'internal'
  END;

UPDATE pois p
JOIN spots s ON s.legacy_place_group_id = p.place_group_id
SET p.spot_id = s.id
WHERE p.place_group_id IS NOT NULL
  AND p.place_group_id <> 100;

UPDATE pois p
JOIN spots s ON s.legacy_poi_id = p.id
SET p.spot_id = s.id
WHERE p.place_group_id = 100;

-- 全国占位数据中与正式校区代表数据重复的旧景点行只做软禁用，不物理删除。
UPDATE pois p
JOIN spots s ON s.legacy_poi_id = p.id
JOIN (
  SELECT official.name
  FROM spots official
  WHERE official.legacy_place_group_id IN (3, 4)
) dup ON dup.name = p.name
SET p.status = 0,
    s.status = 0
WHERE p.place_group_id = 100
  AND p.name IN ('北京邮电大学西土城校区', '北京邮电大学沙河校区');

-- 迁移景点标签。已有 poi 标签如果挂在代表景点行上，复制给 spots。
INSERT IGNORE INTO spot_tag_relations (spot_id, tag_id)
SELECT DISTINCT
  s.id,
  ptr.tag_id
FROM spots s
JOIN poi_tag_relations ptr ON ptr.poi_id = s.representative_poi_id;

-- 从旧 route_edges 的端点生成 route_nodes。
INSERT INTO route_nodes
  (spot_id, legacy_poi_id, name, node_type, visible, map_x, map_y, longitude, latitude, status)
SELECT DISTINCT
  p.spot_id,
  p.id AS legacy_poi_id,
  p.name,
  CASE
    WHEN p.area_code = 'route' THEN 'intersection'
    WHEN c.code = 'gate' THEN 'gate'
    ELSE 'poi'
  END AS node_type,
  CASE WHEN p.area_code = 'route' THEN 0 ELSE 1 END AS visible,
  p.map_x,
  p.map_y,
  p.longitude,
  p.latitude,
  p.status
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
WHERE p.spot_id IS NOT NULL
  AND p.id IN (
    SELECT from_poi_id FROM route_edges
    UNION
    SELECT to_poi_id FROM route_edges
  )
ON DUPLICATE KEY UPDATE
  spot_id = VALUES(spot_id),
  name = VALUES(name),
  node_type = VALUES(node_type),
  visible = VALUES(visible),
  map_x = VALUES(map_x),
  map_y = VALUES(map_y),
  longitude = VALUES(longitude),
  latitude = VALUES(latitude),
  status = VALUES(status);

-- 从旧 route_edges 迁移算法边。
INSERT IGNORE INTO route_node_edges
  (spot_id, from_node_id, to_node_id, distance_m, duration_min, transport_type,
   congestion_factor, is_indoor, description)
SELECT
  fn.spot_id,
  fn.id AS from_node_id,
  tn.id AS to_node_id,
  e.distance_m,
  e.duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  e.description
FROM route_edges e
JOIN route_nodes fn ON fn.legacy_poi_id = e.from_poi_id
JOIN route_nodes tn ON tn.legacy_poi_id = e.to_poi_id
WHERE fn.spot_id = tn.spot_id;

-- 地图底图也补 spot_id，先保留 place_group_id 兼容旧后端。
SET @has_map_spot_id := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'place_group_maps'
    AND COLUMN_NAME = 'spot_id'
);

SET @sql := IF(
  @has_map_spot_id = 0,
  'ALTER TABLE place_group_maps ADD COLUMN spot_id BIGINT DEFAULT NULL COMMENT ''所属景点ID，重构后指向 spots.id'' AFTER id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE place_group_maps pgm
JOIN spots s ON s.legacy_place_group_id = pgm.place_group_id
SET pgm.spot_id = s.id;

DROP VIEW IF EXISTS v_spot_search;
CREATE VIEW v_spot_search AS
SELECT
  s.id AS spot_id,
  s.representative_poi_id AS compat_poi_id,
  s.name,
  s.short_name,
  c.code AS category,
  c.name AS category_name,
  s.spot_type,
  s.province,
  s.city,
  s.district,
  s.address,
  s.description,
  s.cover_image,
  s.longitude,
  s.latitude,
  s.rating,
  s.hotness,
  s.visit_count,
  s.status
FROM spots s
LEFT JOIN poi_categories c ON c.id = s.category_id;

SELECT
  (SELECT COUNT(*) FROM spots) AS spot_count,
  (SELECT COUNT(*) FROM pois WHERE poi_role = 'internal') AS internal_poi_count,
  (SELECT COUNT(*) FROM pois WHERE poi_role = 'route_legacy') AS legacy_route_poi_count,
  (SELECT COUNT(*) FROM route_nodes) AS route_node_count,
  (SELECT COUNT(*) FROM route_node_edges) AS route_node_edge_count;
