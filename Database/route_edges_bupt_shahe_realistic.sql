-- 北京邮电大学沙河校区真实拓扑版路线重构
-- 依据：北邮沙河平面图.jpg
-- 作用：
-- 1. 补充“路口/入口型 POI”作为 Dijkstra 图节点。
-- 2. 清理沙河校区旧 route_edges。
-- 3. 按示意图中的主路、支路、入口连接重建双向路线边。
--
-- 注意：
-- - area_code = 'route' 的节点仅用于路线算法，不在前端起点/终点选择器中展示。
-- - 经纬度为基于现有 POI 坐标和示意图的课设级近似坐标，不是高精度 GIS 数据。

USE tourist_system;

START TRANSACTION;

INSERT INTO pois
  (place_group_id, category_id, name, scene, area_code, area_name, address, location_text, description, longitude, latitude, rating, hotness, visit_count, status)
SELECT
  4,
  (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1),
  node_data.name,
  'campus',
  'route',
  '路线节点',
  '北京邮电大学沙河校区',
  '沙河校区路线节点',
  '根据沙河校区平面图补充的路线规划节点',
  node_data.longitude,
  node_data.latitude,
  5.0,
  0,
  0,
  1
FROM (
  SELECT '西校门内侧路口' AS name, 116.284450 AS longitude, 40.158300 AS latitude
  UNION ALL SELECT '校医院路口', 116.284900, 40.158250
  UNION ALL SELECT '鸿雁路西段路口', 116.285400, 40.158250
  UNION ALL SELECT '鸿雁路国脉路口', 116.286000, 40.158200
  UNION ALL SELECT '甲子钟路口', 116.286750, 40.158200
  UNION ALL SELECT '图书馆西侧路口', 116.286350, 40.158650
  UNION ALL SELECT '图书馆南侧路口', 116.286550, 40.158250
  UNION ALL SELECT '图书馆东侧路口', 116.287200, 40.158450
  UNION ALL SELECT '教学区北侧路口', 116.286900, 40.158850
  UNION ALL SELECT '教学区南侧路口', 116.286900, 40.158500
  UNION ALL SELECT '智慧教学楼路口', 116.287550, 40.158550
  UNION ALL SELECT '东侧学院路口', 116.288200, 40.158550
  UNION ALL SELECT '雁北园路口', 116.285000, 40.159750
  UNION ALL SELECT '北区生活路口', 116.285300, 40.159550
  UNION ALL SELECT '北门内侧路口', 116.286700, 40.160200
  UNION ALL SELECT '风味餐厅路口', 116.285700, 40.157200
  UNION ALL SELECT '南区生活路口', 116.285550, 40.157000
  UNION ALL SELECT '学生活动中心路口', 116.285900, 40.157750
  UNION ALL SELECT '下沉广场路口', 116.286650, 40.157600
  UNION ALL SELECT '南门内侧路口', 116.286900, 40.156980
  UNION ALL SELECT '西侧运动区路口', 116.285300, 40.157600
  UNION ALL SELECT '东体育场路口', 116.288200, 40.157800
  UNION ALL SELECT '东校门内侧路口', 116.288900, 40.158400
  UNION ALL SELECT '工程训练中心路口', 116.287950, 40.157900
  UNION ALL SELECT '南侧体育路口', 116.288500, 40.157100
) AS node_data
WHERE NOT EXISTS (
  SELECT 1
  FROM pois p
  WHERE p.place_group_id = 4
    AND p.name = node_data.name
);

UPDATE pois p
JOIN (
  SELECT '西校门内侧路口' AS name, 116.284450 AS longitude, 40.158300 AS latitude
  UNION ALL SELECT '校医院路口', 116.284900, 40.158250
  UNION ALL SELECT '鸿雁路西段路口', 116.285400, 40.158250
  UNION ALL SELECT '鸿雁路国脉路口', 116.286000, 40.158200
  UNION ALL SELECT '甲子钟路口', 116.286750, 40.158200
  UNION ALL SELECT '图书馆西侧路口', 116.286350, 40.158650
  UNION ALL SELECT '图书馆南侧路口', 116.286550, 40.158250
  UNION ALL SELECT '图书馆东侧路口', 116.287200, 40.158450
  UNION ALL SELECT '教学区北侧路口', 116.286900, 40.158850
  UNION ALL SELECT '教学区南侧路口', 116.286900, 40.158500
  UNION ALL SELECT '智慧教学楼路口', 116.287550, 40.158550
  UNION ALL SELECT '东侧学院路口', 116.288200, 40.158550
  UNION ALL SELECT '雁北园路口', 116.285000, 40.159750
  UNION ALL SELECT '北区生活路口', 116.285300, 40.159550
  UNION ALL SELECT '北门内侧路口', 116.286700, 40.160200
  UNION ALL SELECT '风味餐厅路口', 116.285700, 40.157200
  UNION ALL SELECT '南区生活路口', 116.285550, 40.157000
  UNION ALL SELECT '学生活动中心路口', 116.285900, 40.157750
  UNION ALL SELECT '下沉广场路口', 116.286650, 40.157600
  UNION ALL SELECT '南门内侧路口', 116.286900, 40.156980
  UNION ALL SELECT '西侧运动区路口', 116.285300, 40.157600
  UNION ALL SELECT '东体育场路口', 116.288200, 40.157800
  UNION ALL SELECT '东校门内侧路口', 116.288900, 40.158400
  UNION ALL SELECT '工程训练中心路口', 116.287950, 40.157900
  UNION ALL SELECT '南侧体育路口', 116.288500, 40.157100
) node_data ON node_data.name = p.name
SET p.longitude = node_data.longitude,
    p.latitude = node_data.latitude,
    p.area_code = 'route',
    p.area_name = '路线节点',
    p.description = '根据沙河校区平面图补充的路线规划节点',
    p.status = 1
WHERE p.place_group_id = 4;

DELETE e
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;

CREATE TEMPORARY TABLE tmp_shahe_route_edges (
  from_name VARCHAR(100) NOT NULL,
  to_name VARCHAR(100) NOT NULL,
  distance_m INT NOT NULL,
  duration_min INT NOT NULL,
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
  is_indoor TINYINT NOT NULL DEFAULT 0,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_shahe_route_edges
  (from_name, to_name, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
VALUES
  -- 西侧与鸿雁路主轴
  ('西校门', '西校门内侧路口', 35, 1, 'walk', 1.00, 0, '西校门进入校园内部道路'),
  ('西校门内侧路口', '校医院路口', 65, 1, 'walk', 1.00, 0, '西校门内侧路口到校医院路口'),
  ('校医院路口', '鸿雁路西段路口', 85, 2, 'walk', 1.00, 0, '沿鸿雁路西段前往主路'),
  ('鸿雁路西段路口', '鸿雁路国脉路口', 90, 2, 'walk', 1.00, 0, '沿鸿雁路向东到国脉路口'),
  ('鸿雁路国脉路口', '甲子钟路口', 95, 2, 'walk', 1.00, 0, '沿鸿雁路向东到甲子钟路口'),
  ('甲子钟路口', '图书馆南侧路口', 80, 1, 'walk', 1.00, 0, '甲子钟路口到图书馆南侧路口'),
  ('图书馆南侧路口', '图书馆东侧路口', 95, 2, 'walk', 1.00, 0, '图书馆南侧路口到东侧路口'),
  ('图书馆东侧路口', '智慧教学楼路口', 110, 2, 'walk', 1.00, 0, '图书馆东侧路口到智慧教学楼路口'),
  ('智慧教学楼路口', '东侧学院路口', 120, 2, 'walk', 1.00, 0, '智慧教学楼路口到东侧学院路口'),
  ('东侧学院路口', '东校门内侧路口', 130, 2, 'walk', 1.00, 0, '东侧学院路口到东校门内侧路口'),
  ('东校门内侧路口', '东校门', 60, 1, 'walk', 1.00, 0, '东校门内侧路口到东校门'),

  -- 北侧生活区与北门
  ('北门', '北门内侧路口', 45, 1, 'walk', 1.00, 0, '北门进入校园'),
  ('北门内侧路口', '雁北园路口', 95, 2, 'walk', 1.00, 0, '北门内侧路口到雁北园路口'),
  ('雁北园路口', '北区生活路口', 70, 1, 'walk', 1.00, 0, '雁北园路口到北区生活路口'),
  ('北区生活路口', '鸿雁路西段路口', 175, 3, 'walk', 1.00, 0, '北区生活路口南下至鸿雁路'),

  -- 教学区与图书馆主轴
  ('图书馆西侧路口', '图书馆南侧路口', 75, 1, 'walk', 1.00, 0, '图书馆西侧路口到南侧路口'),
  ('图书馆西侧路口', '教学区北侧路口', 85, 2, 'walk', 1.00, 0, '图书馆西侧路口到教学区北侧路口'),
  ('教学区北侧路口', '教学区南侧路口', 75, 1, 'walk', 1.00, 0, '教学区北侧路口到南侧路口'),
  ('教学区南侧路口', '甲子钟路口', 90, 2, 'walk', 1.00, 0, '教学区南侧路口到甲子钟路口'),
  ('教学区南侧路口', '智慧教学楼路口', 110, 2, 'walk', 1.00, 0, '教学区南侧路口到智慧教学楼路口'),

  -- 南侧生活服务区和南门
  ('鸿雁路西段路口', '风味餐厅路口', 145, 3, 'walk', 1.00, 0, '鸿雁路西段路口南下到风味餐厅路口'),
  ('风味餐厅路口', '南区生活路口', 80, 1, 'walk', 1.00, 0, '风味餐厅路口到南区生活路口'),
  ('风味餐厅路口', '学生活动中心路口', 110, 2, 'walk', 1.00, 0, '风味餐厅路口到学生活动中心路口'),
  ('学生活动中心路口', '下沉广场路口', 120, 2, 'walk', 1.00, 0, '学生活动中心路口到下沉广场路口'),
  ('下沉广场路口', '南门内侧路口', 160, 3, 'walk', 1.00, 0, '下沉广场路口到南门内侧路口'),
  ('南门内侧路口', '南校门', 50, 1, 'walk', 1.00, 0, '南门内侧路口到南校门'),
  ('下沉广场路口', '工程训练中心路口', 170, 3, 'walk', 1.00, 0, '下沉广场路口到工程训练中心路口'),
  ('工程训练中心路口', '东体育场路口', 120, 2, 'walk', 1.00, 0, '工程训练中心路口到东体育场路口'),
  ('东体育场路口', '南侧体育路口', 120, 2, 'walk', 1.00, 0, '东体育场路口到南侧体育路口'),
  ('南侧体育路口', '南门内侧路口', 180, 3, 'walk', 1.00, 0, '南侧体育路口到南门内侧路口'),
  ('南区生活路口', '西侧运动区路口', 120, 2, 'walk', 1.00, 0, '南区生活路口到西侧运动区路口'),
  ('西侧运动区路口', '西校门内侧路口', 155, 3, 'walk', 1.00, 0, '西侧运动区路口到西校门内侧路口'),

  -- 建筑、食堂、服务点接入真实路网
  ('校医院', '校医院路口', 30, 1, 'walk', 1.00, 0, '校医院接入校医院路口'),
  ('师生综合服务大厅', '下沉广场路口', 60, 1, 'walk', 1.00, 0, '师生综合服务大厅接入下沉广场路口'),
  ('学生活动中心', '学生活动中心路口', 35, 1, 'walk', 1.00, 0, '学生活动中心接入活动中心路口'),
  ('下沉广场', '下沉广场路口', 30, 1, 'walk', 1.00, 0, '下沉广场接入下沉广场路口'),
  ('甲子钟广场', '甲子钟路口', 35, 1, 'walk', 1.00, 0, '甲子钟广场接入甲子钟路口'),
  ('沙河校区图书馆', '图书馆西侧路口', 45, 1, 'walk', 1.00, 0, '图书馆西侧入口接入路网'),
  ('沙河校区图书馆', '图书馆南侧路口', 55, 1, 'walk', 1.00, 0, '图书馆南侧入口接入路网'),
  ('教学实验综合楼 N 楼', '教学区北侧路口', 45, 1, 'walk', 1.00, 0, '教学实验综合楼 N 楼接入教学区北侧路口'),
  ('教学实验综合楼 S 楼', '教学区南侧路口', 40, 1, 'walk', 1.00, 0, '教学实验综合楼 S 楼接入教学区南侧路口'),
  ('智慧教学楼', '智慧教学楼路口', 45, 1, 'walk', 1.00, 0, '智慧教学楼接入东侧主路'),
  ('综合办公楼', '下沉广场路口', 90, 2, 'walk', 1.00, 0, '综合办公楼接入下沉广场路口'),
  ('网络空间安全学院楼', '东侧学院路口', 75, 1, 'walk', 1.00, 0, '网安学院楼接入东侧学院路口'),
  ('数字媒体与设计艺术学院楼', '东侧学院路口', 65, 1, 'walk', 1.00, 0, '数媒学院楼接入东侧学院路口'),
  ('工程训练中心', '工程训练中心路口', 55, 1, 'walk', 1.00, 0, '工程训练中心接入南侧路网'),
  ('东体育场', '东体育场路口', 60, 1, 'walk', 1.00, 0, '东体育场接入体育区路口'),
  ('体育馆（在建）', '南侧体育路口', 70, 1, 'walk', 1.00, 0, '体育馆接入南侧体育路口'),
  ('西体育场', '西侧运动区路口', 60, 1, 'walk', 1.00, 0, '西体育场接入运动区路口'),
  ('篮球场', '南侧体育路口', 55, 1, 'walk', 1.00, 0, '篮球场接入南侧体育路口'),
  ('排球场', '南侧体育路口', 60, 1, 'walk', 1.00, 0, '排球场接入南侧体育路口'),
  ('网球场', '南侧体育路口', 75, 1, 'walk', 1.00, 0, '网球场接入南侧体育路口'),
  ('羽毛球场', '南侧体育路口', 85, 2, 'walk', 1.00, 0, '羽毛球场接入南侧体育路口'),
  ('乒乓球场', '南侧体育路口', 90, 2, 'walk', 1.00, 0, '乒乓球场接入南侧体育路口'),
  ('健身房', '南侧体育路口', 65, 1, 'walk', 1.00, 0, '健身房接入南侧体育路口'),
  ('北区食堂', '北区生活路口', 45, 1, 'walk', 1.00, 0, '北区食堂接入北区生活路口'),
  ('雁北园学生公寓', '雁北园路口', 40, 1, 'walk', 1.00, 0, '雁北园学生公寓接入雁北园路口'),
  ('留学生公寓', '鸿雁路西段路口', 90, 2, 'walk', 1.00, 0, '留学生公寓接入鸿雁路西段'),
  ('风味餐厅', '风味餐厅路口', 30, 1, 'walk', 1.00, 0, '风味餐厅接入风味餐厅路口'),
  ('教工餐厅', '风味餐厅路口', 60, 1, 'walk', 1.00, 0, '教工餐厅接入风味餐厅路口'),
  ('南区食堂', '南区生活路口', 45, 1, 'walk', 1.00, 0, '南区食堂接入南区生活路口'),
  ('雁南园学生公寓', '南区生活路口', 55, 1, 'walk', 1.00, 0, '雁南园学生公寓接入南区生活路口'),
  ('快递中心', '西侧运动区路口', 70, 1, 'walk', 1.00, 0, '快递中心接入西侧运动区路口'),
  ('校园超市', '南区生活路口', 65, 1, 'walk', 1.00, 0, '校园超市接入南区生活路口'),
  ('咖啡店', '南区生活路口', 75, 1, 'walk', 1.00, 0, '咖啡店接入南区生活路口'),
  ('洗衣房', '西侧运动区路口', 80, 1, 'walk', 1.00, 0, '洗衣房接入西侧运动区路口'),
  ('理发店', '西侧运动区路口', 80, 1, 'walk', 1.00, 0, '理发店接入西侧运动区路口'),
  ('打印店', '南区生活路口', 65, 1, 'walk', 1.00, 0, '打印店接入南区生活路口'),
  ('中心绿地', '甲子钟路口', 65, 1, 'walk', 1.00, 0, '中心绿地接入甲子钟路口'),
  ('景观湖', '下沉广场路口', 90, 2, 'walk', 1.00, 0, '景观湖接入下沉广场路口'),
  ('友谊林', '教学区北侧路口', 95, 2, 'walk', 1.00, 0, '友谊林接入教学区北侧路口'),

  -- 室内/连廊可选边
  ('沙河校区图书馆', '共享自习室', 120, 2, 'indoor', 1.00, 1, '图书馆到共享自习室室内路线'),
  ('共享自习室', '共享研讨室', 80, 1, 'indoor', 1.00, 1, '共享自习室到共享研讨室'),
  ('教学实验综合楼 N 楼', '教学实验综合楼 S 楼', 130, 2, 'indoor', 1.00, 1, '教学实验综合楼 N 楼到 S 楼连廊'),
  ('教学实验综合楼 S 楼', '公共教学楼 S1 楼', 170, 3, 'indoor', 1.00, 1, '教学实验综合楼 S 楼到公共教学楼 S1 楼'),
  ('公共教学楼 S1 楼', '公共教学楼 S2 楼', 100, 2, 'indoor', 1.00, 1, '公共教学楼 S1 楼到 S2 楼'),
  ('公共教学楼 S2 楼', '公共教学楼 S3 楼', 100, 2, 'indoor', 1.00, 1, '公共教学楼 S2 楼到 S3 楼');

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  p_from.id,
  p_to.id,
  e.distance_m,
  e.duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  e.description
FROM tmp_shahe_route_edges e
JOIN pois p_from ON p_from.place_group_id = 4 AND p_from.name = e.from_name
JOIN pois p_to ON p_to.place_group_id = 4 AND p_to.name = e.to_name;

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  p_to.id,
  p_from.id,
  e.distance_m,
  e.duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  CONCAT(e.to_name, '返回', e.from_name)
FROM tmp_shahe_route_edges e
JOIN pois p_from ON p_from.place_group_id = 4 AND p_from.name = e.from_name
JOIN pois p_to ON p_to.place_group_id = 4 AND p_to.name = e.to_name;

DROP TEMPORARY TABLE tmp_shahe_route_edges;

COMMIT;

SELECT COUNT(*) AS shahe_route_edges
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;
