-- 优化沙河校区南门到西门的示意图路网
-- 目的：
-- - 按平面图补充南门到西侧主路的 L 型路径。
-- - 避免 Dijkstra 从南门到西门绕行下沉广场、学生活动中心、风味餐厅。
-- - 不删除 POI，只新增隐藏路线节点和补充路线边。

USE tourist_system;

SET @shahe_id := 4;
SET @route_category_id := (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1);

INSERT INTO pois
  (place_group_id, category_id, name, scene, area_code, area_name, address, location_text,
   description, longitude, latitude, map_x, map_y, rating, hotness, visit_count, status)
SELECT
  @shahe_id,
  @route_category_id,
  '西南角路口',
  'campus',
  'route',
  '路线节点',
  '北京邮电大学沙河校区西南侧道路',
  '沙河校区西南角道路节点',
  '用于路线算法的隐藏路口节点',
  116.284450,
  40.156980,
  170,
  1500,
  5.0,
  0,
  0,
  1
WHERE NOT EXISTS (
  SELECT 1
  FROM pois
  WHERE place_group_id = @shahe_id
    AND name = '西南角路口'
);

CREATE TEMPORARY TABLE tmp_shahe_south_west_edges (
  from_name VARCHAR(100) NOT NULL,
  to_name VARCHAR(100) NOT NULL,
  distance_m INT NOT NULL,
  duration_min INT NOT NULL,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_shahe_south_west_edges
  (from_name, to_name, distance_m, duration_min, description)
VALUES
  ('南门内侧路口', '西南角路口', 260, 5, '沿南侧道路向西到西南角路口'),
  ('西南角路口', '西校门内侧路口', 220, 4, '沿西侧道路向北到西校门内侧路口');

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  fp.id,
  tp.id,
  e.distance_m,
  e.duration_min,
  'walk',
  1.00,
  0,
  e.description
FROM tmp_shahe_south_west_edges e
JOIN pois fp ON fp.place_group_id = @shahe_id AND fp.name = e.from_name
JOIN pois tp ON tp.place_group_id = @shahe_id AND tp.name = e.to_name
WHERE NOT EXISTS (
  SELECT 1
  FROM route_edges existing
  WHERE existing.from_poi_id = fp.id
    AND existing.to_poi_id = tp.id
);

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  tp.id,
  fp.id,
  e.distance_m,
  e.duration_min,
  'walk',
  1.00,
  0,
  CONCAT(e.to_name, '返回', e.from_name)
FROM tmp_shahe_south_west_edges e
JOIN pois fp ON fp.place_group_id = @shahe_id AND fp.name = e.from_name
JOIN pois tp ON tp.place_group_id = @shahe_id AND tp.name = e.to_name
WHERE NOT EXISTS (
  SELECT 1
  FROM route_edges existing
  WHERE existing.from_poi_id = tp.id
    AND existing.to_poi_id = fp.id
);

DROP TEMPORARY TABLE tmp_shahe_south_west_edges;

SELECT
  p.name,
  p.map_x,
  p.map_y
FROM pois p
WHERE p.place_group_id = @shahe_id
  AND p.name IN ('南门内侧路口', '西南角路口', '西校门内侧路口')
ORDER BY p.name;
