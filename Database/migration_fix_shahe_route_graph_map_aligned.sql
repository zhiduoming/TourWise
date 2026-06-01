-- 北京邮电大学沙河校区路线图修正版
-- 目标：
-- 1. 使用平面图像素坐标 map_x/map_y 作为本地路线规划的主依据。
-- 2. 重建沙河校区 route_edges，避免“跨楼/跨操场直连”的假边。
-- 3. distance_m 由平面图像素距离按 0.35m/px 估算，重点保证相对权重可信。

USE tourist_system;

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_shahe_points (
  name VARCHAR(100) PRIMARY KEY,
  area_code VARCHAR(50) NOT NULL,
  area_name VARCHAR(100) NOT NULL,
  category_code VARCHAR(50) NOT NULL,
  map_x INT NOT NULL,
  map_y INT NOT NULL,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_shahe_points
  (name, area_code, area_name, category_code, map_x, map_y, description)
VALUES
  -- 用户可选 POI 坐标校准
  ('西校门', 'gate', '校门', 'scenic', 155, 930, '沙河校区西校门'),
  ('南校门', 'gate', '校门', 'scenic', 1185, 1510, '沙河校区南校门'),
  ('东校门', 'gate', '校门', 'scenic', 1885, 920, '沙河校区东校门'),
  ('北校门', 'gate', '校门', 'scenic', 1185, 190, '沙河校区北校门'),
  ('体育馆（在建）', 'sports', '体育场馆', 'sports', 310, 720, '沙河校区体育馆'),
  ('西体育场', 'sports', '体育场馆', 'sports', 430, 1045, '沙河校区西侧运动场'),
  ('东体育场', 'sports', '体育场馆', 'sports', 1785, 1180, '沙河校区东体育场'),
  ('沙河校区图书馆', 'teaching', '教学科研', 'library', 1390, 760, '沙河校区图书馆'),
  ('甲子钟广场', 'scenic', '景观', 'scenic', 1110, 920, '沙河校区甲子钟广场'),
  ('北区食堂', 'living', '生活服务', 'food', 720, 365, '沙河校区北区食堂'),
  ('风味餐厅', 'living', '生活服务', 'food', 900, 675, '沙河校区风味餐厅'),
  ('教工餐厅', 'living', '生活服务', 'food', 900, 825, '沙河校区教工餐厅'),
  ('南区食堂', 'living', '生活服务', 'food', 720, 1265, '沙河校区南区食堂'),
  ('学生活动中心', 'living', '生活服务', 'service', 1065, 835, '沙河校区学生活动中心'),
  ('师生综合服务大厅', 'service', '服务', 'service', 1000, 890, '沙河校区师生综合服务大厅'),
  ('综合办公楼', 'office', '办公', 'office', 1070, 665, '沙河校区综合办公楼'),
  ('校医院', 'service', '服务', 'service', 555, 760, '沙河校区校医院'),
  ('快递中心', 'living', '生活服务', 'service', 175, 870, '沙河校区快递中心'),
  ('校园超市', 'living', '生活服务', 'service', 1000, 795, '沙河校区校园超市'),
  ('咖啡店', 'living', '生活服务', 'food', 1035, 720, '沙河校区咖啡店'),
  ('打印店', 'living', '生活服务', 'service', 1020, 825, '沙河校区打印店'),
  ('理发店', 'living', '生活服务', 'service', 585, 790, '沙河校区理发店'),
  ('洗衣房', 'living', '生活服务', 'service', 585, 805, '沙河校区洗衣房'),
  ('雁北园学生公寓', 'living', '生活服务', 'service', 640, 675, '沙河校区雁北园学生公寓'),
  ('雁南园学生公寓', 'living', '生活服务', 'service', 635, 1110, '沙河校区雁南园学生公寓'),
  ('留学生公寓', 'living', '生活服务', 'service', 610, 330, '沙河校区留学生公寓'),
  ('教学实验综合楼 N 楼', 'teaching', '教学科研', 'teaching', 1515, 1045, '沙河校区教学实验综合楼 N 楼'),
  ('教学实验综合楼 S 楼', 'teaching', '教学科研', 'teaching', 1515, 1128, '沙河校区教学实验综合楼 S 楼'),
  ('智慧教学楼', 'teaching', '教学科研', 'teaching', 1740, 1035, '沙河校区智慧教学楼'),
  ('网络空间安全学院楼', 'teaching', '教学科研', 'teaching', 1685, 700, '沙河校区网络空间安全学院楼'),
  ('数字媒体与设计艺术学院楼', 'teaching', '教学科研', 'teaching', 1695, 900, '沙河校区数字媒体与设计艺术学院楼'),
  ('工程训练中心', 'teaching', '教学科研', 'teaching', 1740, 1198, '沙河校区工程训练中心'),
  ('中心绿地', 'scenic', '景观', 'scenic', 1185, 790, '沙河校区中心绿地'),
  ('景观湖', 'scenic', '景观', 'scenic', 1395, 1245, '沙河校区景观湖'),
  ('友谊林', 'scenic', '景观', 'scenic', 1420, 435, '沙河校区友谊林'),
  ('下沉广场', 'scenic', '景观', 'scenic', 1415, 1125, '沙河校区下沉广场'),

  -- 仅用于路线计算的路口节点，不在前端起终点选择器展示
  ('西校门内侧路口', 'route', '路线节点', 'scenic', 170, 925, '西校门内侧主路口'),
  ('体育馆路口', 'route', '路线节点', 'scenic', 170, 720, '体育馆西侧道路路口'),
  ('西南角路口', 'route', '路线节点', 'scenic', 170, 1500, '西南角道路路口'),
  ('校医院路口', 'route', '路线节点', 'scenic', 610, 760, '校医院东侧道路路口'),
  ('北侧主路西口', 'route', '路线节点', 'scenic', 610, 190, '北侧主路西口'),
  ('北区生活路口', 'route', '路线节点', 'scenic', 610, 410, '北区生活服务区路口'),
  ('雁北园路口', 'route', '路线节点', 'scenic', 610, 675, '雁北园学生公寓路口'),
  ('鸿雁路西段路口', 'route', '路线节点', 'scenic', 610, 925, '鸿雁路西段路口'),
  ('南区生活路口', 'route', '路线节点', 'scenic', 610, 1220, '南区生活区路口'),
  ('风味餐厅路口', 'route', '路线节点', 'scenic', 815, 675, '风味餐厅西侧路口'),
  ('鸿雁路国脉路口', 'route', '路线节点', 'scenic', 815, 925, '国脉路与鸿雁路路口'),
  ('公共教学楼西路口', 'route', '路线节点', 'scenic', 815, 1125, '公共教学楼西侧路口'),
  ('综合办公楼路口', 'route', '路线节点', 'scenic', 1030, 675, '综合办公楼西侧路口'),
  ('学生活动中心路口', 'route', '路线节点', 'scenic', 1030, 835, '学生活动中心西侧路口'),
  ('北门内侧路口', 'route', '路线节点', 'scenic', 1185, 190, '北门内侧路口'),
  ('教学区北侧路口', 'route', '路线节点', 'scenic', 1185, 580, '教学区北侧路口'),
  ('图书馆西侧路口', 'route', '路线节点', 'scenic', 1185, 735, '图书馆西侧路口'),
  ('甲子钟路口', 'route', '路线节点', 'scenic', 1185, 925, '甲子钟附近鸿雁路路口'),
  ('下沉广场路口', 'route', '路线节点', 'scenic', 1185, 1125, '下沉广场西侧路口'),
  ('南门内侧路口', 'route', '路线节点', 'scenic', 1185, 1500, '南门内侧路口'),
  ('北侧主路东口', 'route', '路线节点', 'scenic', 1545, 190, '北侧主路东口'),
  ('图书馆东侧路口', 'route', '路线节点', 'scenic', 1545, 735, '图书馆东侧路口'),
  ('智慧教学楼路口', 'route', '路线节点', 'scenic', 1545, 925, '智慧教学楼西侧主路口'),
  ('教学实验楼路口', 'route', '路线节点', 'scenic', 1545, 1125, '教学实验综合楼西侧路口'),
  ('南侧体育路口', 'route', '路线节点', 'scenic', 1545, 1230, '南侧体育区路口'),
  ('东侧学院路口', 'route', '路线节点', 'scenic', 1880, 590, '东侧学院区域路口'),
  ('东校门内侧路口', 'route', '路线节点', 'scenic', 1880, 925, '东校门内侧路口'),
  ('东南学院路口', 'route', '路线节点', 'scenic', 1880, 1230, '东南学院区域路口');

UPDATE pois p
JOIN tmp_shahe_points tp ON tp.name = p.name
SET p.area_code = tp.area_code,
    p.area_name = tp.area_name,
    p.map_x = tp.map_x,
    p.map_y = tp.map_y,
    p.description = tp.description,
    p.status = 1
WHERE p.place_group_id = 4;

INSERT INTO pois
  (place_group_id, category_id, name, scene, area_code, area_name, address, location_text,
   description, map_x, map_y, rating, hotness, visit_count, status)
SELECT
  4,
  COALESCE((SELECT id FROM poi_categories WHERE code = tp.category_code LIMIT 1),
           (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1)),
  tp.name,
  'campus',
  tp.area_code,
  tp.area_name,
  '北京邮电大学沙河校区',
  '沙河校区',
  tp.description,
  tp.map_x,
  tp.map_y,
  5.0,
  0,
  0,
  1
FROM tmp_shahe_points tp
WHERE NOT EXISTS (
  SELECT 1
  FROM pois p
  WHERE p.place_group_id = 4
    AND p.name = tp.name
);

DELETE e
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;

CREATE TEMPORARY TABLE tmp_shahe_edges (
  from_name VARCHAR(100) NOT NULL,
  to_name VARCHAR(100) NOT NULL,
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
  is_indoor TINYINT NOT NULL DEFAULT 0,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_shahe_edges
  (from_name, to_name, transport_type, congestion_factor, is_indoor, description)
VALUES
  -- 主路骨架：尽量沿平面图灰色道路拆成水平/垂直小段，避免跨建筑直连
  ('北侧主路西口', '北门内侧路口', 'walk', 1.00, 0, '沿北侧主路向东'),
  ('北门内侧路口', '北侧主路东口', 'walk', 1.00, 0, '沿北侧主路向东'),
  ('北侧主路西口', '北区生活路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  ('北区生活路口', '雁北园路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  ('雁北园路口', '校医院路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  ('校医院路口', '鸿雁路西段路口', 'walk', 1.00, 0, '沿西侧纵路到鸿雁路'),
  ('鸿雁路西段路口', '南区生活路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  ('南区生活路口', '西南角路口', 'walk', 1.00, 0, '沿西侧纵路到西南角'),

  ('西校门内侧路口', '鸿雁路西段路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('鸿雁路西段路口', '鸿雁路国脉路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('鸿雁路国脉路口', '甲子钟路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('甲子钟路口', '智慧教学楼路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('智慧教学楼路口', '东校门内侧路口', 'walk', 1.00, 0, '沿鸿雁路向东'),

  ('北门内侧路口', '教学区北侧路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('教学区北侧路口', '图书馆西侧路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('图书馆西侧路口', '甲子钟路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('甲子钟路口', '下沉广场路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('下沉广场路口', '南门内侧路口', 'walk', 1.00, 0, '沿中轴路到南门'),

  ('北侧主路东口', '图书馆东侧路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  ('图书馆东侧路口', '智慧教学楼路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  ('智慧教学楼路口', '教学实验楼路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  ('教学实验楼路口', '南侧体育路口', 'walk', 1.00, 0, '沿东侧纵路到体育区'),

  ('北侧主路东口', '东侧学院路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),
  ('东侧学院路口', '东校门内侧路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),
  ('东校门内侧路口', '东南学院路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),

  ('西南角路口', '南门内侧路口', 'walk', 1.00, 0, '沿南侧道路向东'),
  ('南门内侧路口', '南侧体育路口', 'walk', 1.00, 0, '沿南侧道路向东'),
  ('南侧体育路口', '东南学院路口', 'walk', 1.00, 0, '沿南侧道路向东'),

  ('风味餐厅路口', '综合办公楼路口', 'walk', 1.00, 0, '生活服务区北侧横路'),
  ('综合办公楼路口', '图书馆西侧路口', 'walk', 1.00, 0, '综合办公楼到图书馆西侧'),
  ('风味餐厅路口', '鸿雁路国脉路口', 'walk', 1.00, 0, '风味餐厅路口到鸿雁路'),
  ('学生活动中心路口', '甲子钟路口', 'walk', 1.00, 0, '学生活动中心路口到鸿雁路'),
  ('风味餐厅路口', '学生活动中心路口', 'walk', 1.00, 0, '生活服务区中部横路'),
  ('鸿雁路国脉路口', '公共教学楼西路口', 'walk', 1.00, 0, '国脉路向南到公共教学楼'),
  ('公共教学楼西路口', '南区生活路口', 'walk', 1.00, 0, '公共教学楼到南区生活区'),
  ('公共教学楼西路口', '下沉广场路口', 'walk', 1.00, 0, '公共教学楼东侧到下沉广场'),

  -- POI 接入边：只连接到最近且符合平面图道路的路口
  ('西校门', '西校门内侧路口', 'walk', 1.00, 0, '西校门进入校园'),
  ('南校门', '南门内侧路口', 'walk', 1.00, 0, '南校门进入校园'),
  ('东校门', '东校门内侧路口', 'walk', 1.00, 0, '东校门进入校园'),
  ('北校门', '北门内侧路口', 'walk', 1.00, 0, '北校门进入校园'),
  ('体育馆（在建）', '体育馆路口', 'walk', 1.00, 0, '体育馆接入西侧道路'),
  ('体育馆路口', '西校门内侧路口', 'walk', 1.00, 0, '体育馆路口到西校门内侧路口'),
  ('西体育场', '鸿雁路西段路口', 'walk', 1.00, 0, '西体育场接入鸿雁路'),
  ('东体育场', '南侧体育路口', 'walk', 1.00, 0, '东体育场接入南侧体育路口'),
  ('沙河校区图书馆', '图书馆西侧路口', 'walk', 1.00, 0, '图书馆西侧入口'),
  ('沙河校区图书馆', '图书馆东侧路口', 'walk', 1.00, 0, '图书馆东侧入口'),
  ('甲子钟广场', '甲子钟路口', 'walk', 1.00, 0, '甲子钟广场接入鸿雁路'),
  ('北区食堂', '北区生活路口', 'walk', 1.00, 0, '北区食堂接入生活区道路'),
  ('雁北园学生公寓', '雁北园路口', 'walk', 1.00, 0, '雁北园接入道路'),
  ('留学生公寓', '北区生活路口', 'walk', 1.00, 0, '留学生公寓接入北区生活路口'),
  ('风味餐厅', '风味餐厅路口', 'walk', 1.00, 0, '风味餐厅接入道路'),
  ('教工餐厅', '学生活动中心路口', 'walk', 1.00, 0, '教工餐厅接入活动中心路口'),
  ('学生活动中心', '学生活动中心路口', 'walk', 1.00, 0, '学生活动中心接入道路'),
  ('综合办公楼', '综合办公楼路口', 'walk', 1.00, 0, '综合办公楼接入道路'),
  ('校医院', '校医院路口', 'walk', 1.00, 0, '校医院接入道路'),
  ('师生综合服务大厅', '学生活动中心路口', 'walk', 1.00, 0, '服务大厅接入活动中心路口'),
  ('校园超市', '学生活动中心路口', 'walk', 1.00, 0, '校园超市接入活动中心路口'),
  ('咖啡店', '综合办公楼路口', 'walk', 1.00, 0, '咖啡店接入综合办公楼路口'),
  ('打印店', '学生活动中心路口', 'walk', 1.00, 0, '打印店接入活动中心路口'),
  ('理发店', '校医院路口', 'walk', 1.00, 0, '理发店接入校医院路口'),
  ('洗衣房', '校医院路口', 'walk', 1.00, 0, '洗衣房接入校医院路口'),
  ('南区食堂', '南区生活路口', 'walk', 1.00, 0, '南区食堂接入南区生活路口'),
  ('雁南园学生公寓', '南区生活路口', 'walk', 1.00, 0, '雁南园接入南区生活路口'),
  ('快递中心', '西校门内侧路口', 'walk', 1.00, 0, '快递中心接入西门主路'),
  ('教学实验综合楼 N 楼', '教学实验楼路口', 'walk', 1.00, 0, '教学实验综合楼 N 楼接入道路'),
  ('教学实验综合楼 S 楼', '教学实验楼路口', 'walk', 1.00, 0, '教学实验综合楼 S 楼接入道路'),
  ('智慧教学楼', '智慧教学楼路口', 'walk', 1.00, 0, '智慧教学楼接入道路'),
  ('网络空间安全学院楼', '图书馆东侧路口', 'walk', 1.00, 0, '网安学院楼接入图书馆东侧路口'),
  ('数字媒体与设计艺术学院楼', '智慧教学楼路口', 'walk', 1.00, 0, '数媒学院楼接入智慧教学楼路口'),
  ('工程训练中心', '南侧体育路口', 'walk', 1.00, 0, '工程训练中心接入南侧体育路口'),
  ('中心绿地', '甲子钟路口', 'walk', 1.00, 0, '中心绿地接入甲子钟路口'),
  ('景观湖', '南侧体育路口', 'walk', 1.00, 0, '景观湖接入南侧体育路口'),
  ('友谊林', '教学区北侧路口', 'walk', 1.00, 0, '友谊林接入教学区北侧路口'),
  ('下沉广场', '下沉广场路口', 'walk', 1.00, 0, '下沉广场接入道路'),

  -- 室内/连廊类边，保留楼宇内模式价值
  ('沙河校区图书馆', '共享自习室', 'indoor', 1.00, 1, '图书馆到共享自习室室内路线'),
  ('共享自习室', '共享研讨室', 'indoor', 1.00, 1, '共享自习室到共享研讨室'),
  ('教学实验综合楼 N 楼', '教学实验综合楼 S 楼', 'indoor', 1.00, 1, '教学实验综合楼 N 楼到 S 楼连廊'),
  ('教学实验综合楼 S 楼', '公共教学楼 S1 楼', 'indoor', 1.00, 1, '教学实验综合楼 S 楼到公共教学楼 S1 楼'),
  ('公共教学楼 S1 楼', '公共教学楼 S2 楼', 'indoor', 1.00, 1, '公共教学楼 S1 楼到 S2 楼'),
  ('公共教学楼 S2 楼', '公共教学楼 S3 楼', 'indoor', 1.00, 1, '公共教学楼 S2 楼到 S3 楼');

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  fp.id,
  tp.id,
  GREATEST(10, ROUND(SQRT(POW(tp.map_x - fp.map_x, 2) + POW(tp.map_y - fp.map_y, 2)) * 0.35)) AS distance_m,
  GREATEST(1, CEIL(GREATEST(10, ROUND(SQRT(POW(tp.map_x - fp.map_x, 2) + POW(tp.map_y - fp.map_y, 2)) * 0.35)) / 80)) AS duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  e.description
FROM tmp_shahe_edges e
JOIN pois fp ON fp.place_group_id = 4 AND fp.name = e.from_name
JOIN pois tp ON tp.place_group_id = 4 AND tp.name = e.to_name
WHERE fp.status = 1
  AND tp.status = 1
  AND fp.map_x IS NOT NULL
  AND fp.map_y IS NOT NULL
  AND tp.map_x IS NOT NULL
  AND tp.map_y IS NOT NULL;

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  tp.id,
  fp.id,
  e.distance_m,
  e.duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  CONCAT('返回：', e.description)
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4
  AND NOT EXISTS (
    SELECT 1
    FROM route_edges reverse_edge
    WHERE reverse_edge.from_poi_id = tp.id
      AND reverse_edge.to_poi_id = fp.id
  );

COMMIT;

SELECT
  COUNT(*) AS shahe_route_edges,
  MIN(distance_m) AS min_distance,
  MAX(distance_m) AS max_distance
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;
