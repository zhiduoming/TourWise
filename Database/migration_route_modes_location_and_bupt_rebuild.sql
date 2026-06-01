-- 路线规划模式拆分 + 定位解析字段 + 北邮两个校区内部 POI/路网重建
-- 执行前已备份：Database/backups/20260531_170136_before_route_location_rebuild.sql
-- 影响范围：
--   1. place_groups 增加 route_graph_status
--   2. spots 增加 location_radius_m
--   3. 硬删除并重建 place_group_id IN (3, 4) 的内部 POI 与 route_edges

USE tourist_system;

START TRANSACTION;

SET @has_route_graph_status := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'place_groups'
    AND COLUMN_NAME = 'route_graph_status'
);

SET @sql := IF(
  @has_route_graph_status = 0,
  'ALTER TABLE place_groups ADD COLUMN route_graph_status VARCHAR(20) NOT NULL DEFAULT ''none'' COMMENT ''内部路网状态: none/draft/verified'' AFTER sort_order',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_location_radius_m := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'spots'
    AND COLUMN_NAME = 'location_radius_m'
);

SET @sql := IF(
  @has_location_radius_m = 0,
  'ALTER TABLE spots ADD COLUMN location_radius_m INT NOT NULL DEFAULT 500 COMMENT ''定位匹配半径，单位米'' AFTER latitude',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE place_groups
SET route_graph_status = CASE WHEN id IN (3, 4) THEN 'verified' ELSE 'none' END;

UPDATE spots
SET location_radius_m = CASE
  WHEN legacy_place_group_id IN (3, 4, 5, 6) THEN 900
  WHEN spot_type IN ('campus', 'university', 'park', 'scenic') THEN 800
  WHEN spot_type IN ('museum', 'landmark') THEN 250
  ELSE 500
END;

UPDATE spots
SET representative_poi_id = NULL
WHERE legacy_place_group_id IN (3, 4);

DELETE e
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id IN (3, 4)
  AND tp.place_group_id IN (3, 4);

DELETE FROM pois
WHERE place_group_id IN (3, 4);

CREATE TEMPORARY TABLE tmp_bupt_pois (
  place_group_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  category_code VARCHAR(50) NOT NULL,
  area_code VARCHAR(50) NOT NULL,
  area_name VARCHAR(50) NOT NULL,
  map_x INT NOT NULL,
  map_y INT NOT NULL,
  visible TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(255) NOT NULL,
  PRIMARY KEY (place_group_id, name)
);

INSERT INTO tmp_bupt_pois
  (place_group_id, name, category_code, area_code, area_name, map_x, map_y, visible, description)
VALUES
  -- 北京邮电大学西土城校区，基于“北邮本部平面图.JPG”人工标定
  (3, '北京邮电大学西土城校区', 'university', 'campus_summary', '校区', 1245, 1755, 1, '北京邮电大学西土城校区总览点'),
  (3, '北门', 'gate', 'gate', '校门', 1050, 420, 1, '西土城校区北门'),
  (3, '中门', 'gate', 'gate', '校门', 1050, 2750, 1, '西土城校区中门'),
  (3, '南门', 'gate', 'gate', '校门', 1480, 3320, 1, '西土城校区南门'),
  (3, '西门', 'gate', 'gate', '校门', 250, 2120, 1, '西土城校区西门'),
  (3, '东北门', 'gate', 'gate', '校门', 2250, 610, 1, '西土城校区东北门'),
  (3, '东门', 'gate', 'gate', '校门', 2250, 1440, 1, '西土城校区东门'),
  (3, '主楼', 'teaching', 'teaching', '教学区', 1450, 2150, 1, '西土城校区主楼'),
  (3, '教一楼', 'teaching', 'teaching', '教学区', 1450, 1710, 1, '西土城校区教一楼'),
  (3, '教二楼', 'teaching', 'teaching', '教学区', 1450, 2440, 1, '西土城校区教二楼'),
  (3, '教三楼', 'teaching', 'teaching', '教学区', 760, 2420, 1, '西土城校区教三楼'),
  (3, '教四楼', 'teaching', 'teaching', '教学区', 760, 1710, 1, '西土城校区教四楼'),
  (3, '图书馆', 'library', 'teaching', '教学区', 1450, 1235, 1, '西土城校区图书馆'),
  (3, '档案馆', 'office', 'office', '办公区', 1515, 1180, 1, '西土城校区档案馆'),
  (3, '科研楼', 'teaching', 'teaching', '教学区', 1850, 770, 1, '西土城校区科研楼'),
  (3, '学生活动中心', 'activity', 'service', '服务区', 1250, 760, 1, '西土城校区学生活动中心'),
  (3, '综合食堂', 'cafeteria', 'living', '生活区', 760, 620, 1, '西土城校区综合食堂'),
  (3, '学生食堂', 'cafeteria', 'living', '生活区', 1820, 1010, 1, '西土城校区学生食堂'),
  (3, '综合服务楼', 'service', 'service', '服务区', 1420, 760, 1, '西土城校区综合服务楼'),
  (3, '物业超市', 'shop', 'service', '服务区', 1300, 980, 1, '西土城校区物业超市'),
  (3, '小松林', 'scenic', 'scenic', '景观区', 1180, 1230, 1, '西土城校区小松林'),
  (3, '时光广场', 'scenic', 'scenic', '景观区', 1180, 1430, 1, '西土城校区时光广场'),
  (3, '篮球场', 'sports', 'sports', '运动区', 1780, 1280, 1, '西土城校区篮球场'),
  (3, '网球场排球场', 'sports', 'sports', '运动区', 1990, 1280, 1, '西土城校区网球场排球场'),
  (3, '体育馆', 'sports', 'sports', '运动区', 2030, 1820, 1, '西土城校区体育馆'),
  (3, '游泳馆', 'sports', 'sports', '运动区', 2200, 1850, 1, '西土城校区游泳馆'),
  (3, '校医院', 'medical', 'service', '服务区', 760, 2750, 1, '西土城校区校医院'),
  (3, '经管楼', 'teaching', 'teaching', '教学区', 1250, 620, 1, '西土城校区经管楼'),
  (3, '学一公寓', 'dormitory', 'living', '生活区', 680, 1470, 1, '西土城校区学一公寓'),
  (3, '学二公寓', 'dormitory', 'living', '生活区', 900, 1470, 1, '西土城校区学二公寓'),
  (3, '学三公寓', 'dormitory', 'living', '生活区', 680, 1270, 1, '西土城校区学三公寓'),
  (3, '学四公寓', 'dormitory', 'living', '生活区', 900, 1270, 1, '西土城校区学四公寓'),
  (3, '学五公寓', 'dormitory', 'living', '生活区', 680, 1000, 1, '西土城校区学五公寓'),
  (3, '学八公寓', 'dormitory', 'living', '生活区', 900, 1000, 1, '西土城校区学八公寓'),
  (3, '学十公寓', 'dormitory', 'living', '生活区', 820, 520, 1, '西土城校区学十公寓'),
  (3, '学十一公寓', 'dormitory', 'living', '生活区', 620, 500, 1, '西土城校区学十一公寓'),
  (3, '学十三公寓', 'dormitory', 'living', '生活区', 300, 900, 1, '西土城校区学十三公寓'),
  (3, '北部锦江酒店', 'commercial', 'service', '服务区', 340, 500, 1, '西土城校区北部锦江酒店'),
  (3, '家属区', 'service', 'living', '生活区', 1780, 2760, 1, '西土城校区家属区'),
  (3, '北门内侧路口', 'scenic', 'route', '路线节点', 1050, 520, 0, '北门内侧路线节点'),
  (3, '学生活动中心路口', 'scenic', 'route', '路线节点', 1250, 760, 0, '学生活动中心附近路线节点'),
  (3, '图书馆路口', 'scenic', 'route', '路线节点', 1450, 1235, 0, '图书馆附近路线节点'),
  (3, '中轴路口', 'scenic', 'route', '路线节点', 1450, 1710, 0, '中轴教学区路线节点'),
  (3, '主楼路口', 'scenic', 'route', '路线节点', 1450, 2150, 0, '主楼附近路线节点'),
  (3, '中门内侧路口', 'scenic', 'route', '路线节点', 1050, 2750, 0, '中门内侧路线节点'),
  (3, '南门内侧路口', 'scenic', 'route', '路线节点', 1480, 3250, 0, '南门内侧路线节点'),
  (3, '西门内侧路口', 'scenic', 'route', '路线节点', 280, 2120, 0, '西门内侧路线节点'),
  (3, '东北门内侧路口', 'scenic', 'route', '路线节点', 2200, 610, 0, '东北门内侧路线节点'),
  (3, '东门内侧路口', 'scenic', 'route', '路线节点', 2200, 1440, 0, '东门内侧路线节点'),
  (3, '公寓区路口', 'scenic', 'route', '路线节点', 760, 1000, 0, '公寓区路线节点'),
  (3, '西侧教学路口', 'scenic', 'route', '路线节点', 760, 1710, 0, '西侧教学楼路线节点'),
  (3, '运动区路口', 'scenic', 'route', '路线节点', 2030, 1820, 0, '运动区路线节点'),
  (3, '食堂区路口', 'scenic', 'route', '路线节点', 1820, 1010, 0, '食堂区路线节点'),

  -- 北京邮电大学沙河校区，基于“北邮沙河平面图.jpg”人工标定
  (4, '北京邮电大学沙河校区', 'university', 'campus_summary', '校区', 1024, 990, 1, '北京邮电大学沙河校区总览点'),
  (4, '西校门', 'gate', 'gate', '校门', 155, 930, 1, '沙河校区西校门'),
  (4, '南校门', 'gate', 'gate', '校门', 1185, 1510, 1, '沙河校区南门'),
  (4, '东校门', 'gate', 'gate', '校门', 1885, 920, 1, '沙河校区东门'),
  (4, '北校门', 'gate', 'gate', '校门', 1185, 190, 1, '沙河校区北门'),
  (4, '体育馆（在建）', 'sports', 'sports', '体育区', 310, 720, 1, '沙河校区体育馆'),
  (4, '西体育场', 'sports', 'sports', '体育区', 430, 1045, 1, '沙河校区西体育场'),
  (4, '东体育场', 'sports', 'sports', '体育区', 1785, 1180, 1, '沙河校区东体育场'),
  (4, '沙河校区图书馆', 'library', 'teaching', '教学科研区', 1390, 760, 1, '沙河校区图书馆'),
  (4, '甲子钟广场', 'scenic', 'scenic', '景观区', 1110, 920, 1, '沙河校区甲子钟广场'),
  (4, '北区食堂', 'cafeteria', 'living', '生活区', 720, 365, 1, '沙河校区北区食堂'),
  (4, '风味餐厅', 'cafeteria', 'living', '生活区', 900, 675, 1, '沙河校区风味餐厅'),
  (4, '教工餐厅', 'cafeteria', 'living', '生活区', 900, 825, 1, '沙河校区教工餐厅'),
  (4, '南区食堂', 'cafeteria', 'living', '生活区', 720, 1265, 1, '沙河校区南区食堂'),
  (4, '学生活动中心', 'activity', 'service', '服务区', 1065, 835, 1, '沙河校区学生活动中心'),
  (4, '师生综合服务大厅', 'service', 'service', '服务区', 1000, 890, 1, '沙河校区师生综合服务大厅'),
  (4, '综合办公楼', 'office', 'office', '办公区', 1070, 665, 1, '沙河校区综合办公楼'),
  (4, '公共教学楼', 'teaching', 'teaching', '教学科研区', 1060, 1115, 1, '沙河校区公共教学楼'),
  (4, '校医院', 'medical', 'service', '服务区', 555, 760, 1, '沙河校区校医院'),
  (4, '快递中心', 'service', 'service', '服务区', 175, 870, 1, '沙河校区快递中心'),
  (4, '校园超市', 'shop', 'service', '服务区', 1000, 795, 1, '沙河校区校园超市'),
  (4, '咖啡店', 'cafeteria', 'living', '生活区', 1035, 720, 1, '沙河校区咖啡店'),
  (4, '打印店', 'service', 'service', '服务区', 1020, 825, 1, '沙河校区打印店'),
  (4, '理发店', 'service', 'service', '服务区', 585, 790, 1, '沙河校区理发店'),
  (4, '洗衣房', 'service', 'service', '服务区', 585, 805, 1, '沙河校区洗衣房'),
  (4, '雁北园学生公寓', 'dormitory', 'living', '生活区', 640, 675, 1, '沙河校区雁北园学生公寓'),
  (4, '雁南园学生公寓', 'dormitory', 'living', '生活区', 635, 1110, 1, '沙河校区雁南园学生公寓'),
  (4, '留学生公寓', 'dormitory', 'living', '生活区', 610, 330, 1, '沙河校区留学生公寓'),
  (4, '教学实验综合楼 N 楼', 'teaching', 'teaching', '教学科研区', 1515, 1045, 1, '沙河校区教学实验综合楼 N 楼'),
  (4, '教学实验综合楼 S 楼', 'teaching', 'teaching', '教学科研区', 1515, 1128, 1, '沙河校区教学实验综合楼 S 楼'),
  (4, '智慧教学楼', 'teaching', 'teaching', '教学科研区', 1740, 1035, 1, '沙河校区智慧教学楼'),
  (4, '网络空间安全学院楼', 'college', 'teaching', '教学科研区', 1685, 700, 1, '沙河校区网络空间安全学院楼'),
  (4, '数字媒体与设计艺术学院楼', 'college', 'teaching', '教学科研区', 1695, 900, 1, '沙河校区数字媒体与设计艺术学院楼'),
  (4, '工程训练中心', 'training', 'teaching', '教学科研区', 1740, 1198, 1, '沙河校区工程训练中心'),
  (4, '中心绿地', 'scenic', 'scenic', '景观区', 1185, 790, 1, '沙河校区中心绿地'),
  (4, '景观湖', 'scenic', 'scenic', '景观区', 1395, 1245, 1, '沙河校区景观湖'),
  (4, '友谊林', 'scenic', 'scenic', '景观区', 1420, 435, 1, '沙河校区友谊林'),
  (4, '下沉广场', 'scenic', 'scenic', '景观区', 1415, 1125, 1, '沙河校区下沉广场'),
  (4, '西校门内侧路口', 'scenic', 'route', '路线节点', 170, 925, 0, '西校门内侧主路口'),
  (4, '体育馆路口', 'scenic', 'route', '路线节点', 170, 720, 0, '体育馆西侧道路路口'),
  (4, '西南角路口', 'scenic', 'route', '路线节点', 170, 1500, 0, '西南角道路路口'),
  (4, '校医院路口', 'scenic', 'route', '路线节点', 610, 760, 0, '校医院东侧道路路口'),
  (4, '北侧主路西口', 'scenic', 'route', '路线节点', 610, 190, 0, '北侧主路西口'),
  (4, '北区生活路口', 'scenic', 'route', '路线节点', 610, 410, 0, '北区生活服务区路口'),
  (4, '雁北园路口', 'scenic', 'route', '路线节点', 610, 675, 0, '雁北园学生公寓路口'),
  (4, '鸿雁路西段路口', 'scenic', 'route', '路线节点', 610, 925, 0, '鸿雁路西段路口'),
  (4, '南区生活路口', 'scenic', 'route', '路线节点', 610, 1220, 0, '南区生活区路口'),
  (4, '风味餐厅路口', 'scenic', 'route', '路线节点', 815, 675, 0, '风味餐厅西侧路口'),
  (4, '鸿雁路国脉路口', 'scenic', 'route', '路线节点', 815, 925, 0, '国脉路与鸿雁路路口'),
  (4, '公共教学楼西路口', 'scenic', 'route', '路线节点', 815, 1125, 0, '公共教学楼西侧路口'),
  (4, '综合办公楼路口', 'scenic', 'route', '路线节点', 1030, 675, 0, '综合办公楼西侧路口'),
  (4, '学生活动中心路口', 'scenic', 'route', '路线节点', 1030, 835, 0, '学生活动中心西侧路口'),
  (4, '北门内侧路口', 'scenic', 'route', '路线节点', 1185, 190, 0, '北门内侧路口'),
  (4, '教学区北侧路口', 'scenic', 'route', '路线节点', 1185, 580, 0, '教学区北侧路口'),
  (4, '图书馆西侧路口', 'scenic', 'route', '路线节点', 1185, 735, 0, '图书馆西侧路口'),
  (4, '甲子钟路口', 'scenic', 'route', '路线节点', 1185, 925, 0, '甲子钟附近鸿雁路路口'),
  (4, '下沉广场路口', 'scenic', 'route', '路线节点', 1185, 1125, 0, '下沉广场西侧路口'),
  (4, '南门内侧路口', 'scenic', 'route', '路线节点', 1185, 1500, 0, '南门内侧路口'),
  (4, '北侧主路东口', 'scenic', 'route', '路线节点', 1545, 190, 0, '北侧主路东口'),
  (4, '图书馆东侧路口', 'scenic', 'route', '路线节点', 1545, 735, 0, '图书馆东侧路口'),
  (4, '智慧教学楼路口', 'scenic', 'route', '路线节点', 1545, 925, 0, '智慧教学楼西侧主路口'),
  (4, '教学实验楼路口', 'scenic', 'route', '路线节点', 1545, 1125, 0, '教学实验综合楼西侧路口'),
  (4, '南侧体育路口', 'scenic', 'route', '路线节点', 1545, 1230, 0, '南侧体育区路口'),
  (4, '东侧学院路口', 'scenic', 'route', '路线节点', 1880, 590, 0, '东侧学院区域路口'),
  (4, '东校门内侧路口', 'scenic', 'route', '路线节点', 1880, 925, 0, '东校门内侧路口'),
  (4, '东南学院路口', 'scenic', 'route', '路线节点', 1880, 1230, 0, '东南学院区域路口');

INSERT INTO pois
  (spot_id, poi_role, place_group_id, category_id, name, scene, area_code, area_name,
   province, city, address, location_text, description, longitude, latitude, map_x, map_y,
   rating, hotness, visit_count, status)
SELECT
  s.id,
  CASE WHEN t.area_code = 'campus_summary' THEN 'spot_legacy'
       WHEN t.visible = 0 THEN 'route_legacy'
       ELSE 'internal' END,
  t.place_group_id,
  COALESCE(c.id, (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1)),
  t.name,
  'campus',
  t.area_code,
  t.area_name,
  '北京市',
  '北京市',
  pg.address,
  pg.short_name,
  t.description,
  CASE
    WHEN t.place_group_id = 3 THEN ROUND(116.358000 + (t.map_x - 1245) * 0.00000170, 6)
    ELSE ROUND(116.286500 + (t.map_x - 1024) * 0.00000265, 6)
  END,
  CASE
    WHEN t.place_group_id = 3 THEN ROUND(39.962000 - (t.map_y - 1755) * 0.00000115, 6)
    ELSE ROUND(40.158800 - (t.map_y - 990) * 0.00000220, 6)
  END,
  t.map_x,
  t.map_y,
  4.8,
  CASE WHEN t.area_code = 'campus_summary' THEN 49000 ELSE 1000 END,
  CASE WHEN t.area_code = 'campus_summary' THEN 49000 ELSE 1000 END,
  1
FROM tmp_bupt_pois t
JOIN place_groups pg ON pg.id = t.place_group_id
JOIN spots s ON s.legacy_place_group_id = t.place_group_id
LEFT JOIN poi_categories c ON c.code = t.category_code;

UPDATE spots s
JOIN pois p ON p.place_group_id = s.legacy_place_group_id
          AND p.area_code = 'campus_summary'
SET s.representative_poi_id = p.id
WHERE s.legacy_place_group_id IN (3, 4);

CREATE TEMPORARY TABLE tmp_bupt_edges (
  place_group_id BIGINT NOT NULL,
  from_name VARCHAR(100) NOT NULL,
  to_name VARCHAR(100) NOT NULL,
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
  is_indoor TINYINT NOT NULL DEFAULT 0,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_bupt_edges
  (place_group_id, from_name, to_name, transport_type, congestion_factor, is_indoor, description)
VALUES
  -- 西土城校区路网骨架
  (3, '北门', '北门内侧路口', 'walk', 1.00, 0, '北门进入校园'),
  (3, '北门内侧路口', '学生活动中心路口', 'walk', 1.00, 0, '沿北侧主路向南'),
  (3, '学生活动中心路口', '图书馆路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (3, '图书馆路口', '中轴路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (3, '中轴路口', '主楼路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (3, '主楼路口', '中门内侧路口', 'walk', 1.00, 0, '沿中轴路到中门'),
  (3, '中门内侧路口', '南门内侧路口', 'walk', 1.00, 0, '沿中轴路到南门'),
  (3, '西门', '西门内侧路口', 'walk', 1.00, 0, '西门进入校园'),
  (3, '西门内侧路口', '中门内侧路口', 'walk', 1.00, 0, '西门到中门主路'),
  (3, '东北门', '东北门内侧路口', 'walk', 1.00, 0, '东北门进入校园'),
  (3, '东北门内侧路口', '科研楼', 'walk', 1.00, 0, '东北门到科研楼'),
  (3, '科研楼', '食堂区路口', 'walk', 1.00, 0, '科研楼到食堂区'),
  (3, '食堂区路口', '东门内侧路口', 'walk', 1.00, 0, '食堂区到东门'),
  (3, '东门', '东门内侧路口', 'walk', 1.00, 0, '东门进入校园'),
  (3, '东门内侧路口', '运动区路口', 'walk', 1.00, 0, '东门到运动区'),
  (3, '运动区路口', '南门内侧路口', 'walk', 1.00, 0, '运动区到南门'),
  (3, '公寓区路口', '西侧教学路口', 'walk', 1.00, 0, '公寓区到西侧教学区'),
  (3, '西侧教学路口', '中轴路口', 'walk', 1.00, 0, '西侧教学区到中轴路'),
  (3, '公寓区路口', '图书馆路口', 'walk', 1.00, 0, '公寓区到图书馆'),
  (3, '西门内侧路口', '公寓区路口', 'walk', 1.00, 0, '西门到公寓区'),
  (3, '南门', '南门内侧路口', 'walk', 1.00, 0, '南门进入校园'),
  (3, '主楼', '主楼路口', 'walk', 1.00, 0, '主楼接入路网'),
  (3, '教一楼', '中轴路口', 'walk', 1.00, 0, '教一楼接入路网'),
  (3, '教二楼', '主楼路口', 'walk', 1.00, 0, '教二楼接入路网'),
  (3, '教三楼', '中门内侧路口', 'walk', 1.00, 0, '教三楼接入路网'),
  (3, '教四楼', '西侧教学路口', 'walk', 1.00, 0, '教四楼接入路网'),
  (3, '图书馆', '图书馆路口', 'walk', 1.00, 0, '图书馆接入路网'),
  (3, '档案馆', '图书馆路口', 'walk', 1.00, 0, '档案馆接入路网'),
  (3, '学生活动中心', '学生活动中心路口', 'walk', 1.00, 0, '学生活动中心接入路网'),
  (3, '经管楼', '学生活动中心路口', 'walk', 1.00, 0, '经管楼接入路网'),
  (3, '综合食堂', '学生活动中心路口', 'walk', 1.00, 0, '综合食堂接入路网'),
  (3, '学生食堂', '食堂区路口', 'walk', 1.00, 0, '学生食堂接入路网'),
  (3, '综合服务楼', '学生活动中心路口', 'walk', 1.00, 0, '综合服务楼接入路网'),
  (3, '物业超市', '图书馆路口', 'walk', 1.00, 0, '物业超市接入路网'),
  (3, '小松林', '图书馆路口', 'walk', 1.00, 0, '小松林接入路网'),
  (3, '时光广场', '中轴路口', 'walk', 1.00, 0, '时光广场接入路网'),
  (3, '篮球场', '食堂区路口', 'walk', 1.00, 0, '篮球场接入路网'),
  (3, '网球场排球场', '食堂区路口', 'walk', 1.00, 0, '网球场排球场接入路网'),
  (3, '体育馆', '运动区路口', 'walk', 1.00, 0, '体育馆接入路网'),
  (3, '游泳馆', '运动区路口', 'walk', 1.00, 0, '游泳馆接入路网'),
  (3, '校医院', '中门内侧路口', 'walk', 1.00, 0, '校医院接入路网'),
  (3, '学一公寓', '公寓区路口', 'walk', 1.00, 0, '学一公寓接入路网'),
  (3, '学二公寓', '公寓区路口', 'walk', 1.00, 0, '学二公寓接入路网'),
  (3, '学三公寓', '公寓区路口', 'walk', 1.00, 0, '学三公寓接入路网'),
  (3, '学四公寓', '公寓区路口', 'walk', 1.00, 0, '学四公寓接入路网'),
  (3, '学五公寓', '公寓区路口', 'walk', 1.00, 0, '学五公寓接入路网'),
  (3, '学八公寓', '公寓区路口', 'walk', 1.00, 0, '学八公寓接入路网'),
  (3, '学十公寓', '北门内侧路口', 'walk', 1.00, 0, '学十公寓接入路网'),
  (3, '学十一公寓', '北门内侧路口', 'walk', 1.00, 0, '学十一公寓接入路网'),
  (3, '学十三公寓', '西门内侧路口', 'walk', 1.00, 0, '学十三公寓接入路网'),
  (3, '北部锦江酒店', '北门内侧路口', 'walk', 1.00, 0, '北部锦江酒店接入路网'),
  (3, '家属区', '南门内侧路口', 'walk', 1.00, 0, '家属区接入路网'),
  (3, '教一楼', '主楼', 'indoor', 1.00, 1, '教一楼到主楼室内连通'),
  (3, '教一楼', '教二楼', 'indoor', 1.00, 1, '教一楼到教二楼楼宇连通'),

  -- 沙河校区路网骨架
  (4, '北侧主路西口', '北门内侧路口', 'walk', 1.00, 0, '沿北侧主路向东'),
  (4, '北门内侧路口', '北侧主路东口', 'walk', 1.00, 0, '沿北侧主路向东'),
  (4, '北侧主路西口', '北区生活路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  (4, '北区生活路口', '雁北园路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  (4, '雁北园路口', '校医院路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  (4, '校医院路口', '鸿雁路西段路口', 'walk', 1.00, 0, '沿西侧纵路到鸿雁路'),
  (4, '鸿雁路西段路口', '南区生活路口', 'walk', 1.00, 0, '沿西侧纵路向南'),
  (4, '南区生活路口', '西南角路口', 'walk', 1.00, 0, '沿西侧纵路到西南角'),
  (4, '西校门内侧路口', '鸿雁路西段路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  (4, '鸿雁路西段路口', '鸿雁路国脉路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  (4, '鸿雁路国脉路口', '甲子钟路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  (4, '甲子钟路口', '智慧教学楼路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  (4, '智慧教学楼路口', '东校门内侧路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  (4, '北门内侧路口', '教学区北侧路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (4, '教学区北侧路口', '图书馆西侧路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (4, '图书馆西侧路口', '甲子钟路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (4, '甲子钟路口', '下沉广场路口', 'walk', 1.00, 0, '沿中轴路向南'),
  (4, '下沉广场路口', '南门内侧路口', 'walk', 1.00, 0, '沿中轴路到南门'),
  (4, '北侧主路东口', '图书馆东侧路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  (4, '图书馆东侧路口', '智慧教学楼路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  (4, '智慧教学楼路口', '教学实验楼路口', 'walk', 1.00, 0, '沿东侧纵路向南'),
  (4, '教学实验楼路口', '南侧体育路口', 'walk', 1.00, 0, '沿东侧纵路到体育区'),
  (4, '北侧主路东口', '东侧学院路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),
  (4, '东侧学院路口', '东校门内侧路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),
  (4, '东校门内侧路口', '东南学院路口', 'walk', 1.00, 0, '沿东侧学院道路向南'),
  (4, '西南角路口', '南门内侧路口', 'walk', 1.00, 0, '沿南侧道路向东'),
  (4, '南门内侧路口', '南侧体育路口', 'walk', 1.00, 0, '沿南侧道路向东'),
  (4, '南侧体育路口', '东南学院路口', 'walk', 1.00, 0, '沿南侧道路向东'),
  (4, '风味餐厅路口', '综合办公楼路口', 'walk', 1.00, 0, '生活服务区北侧横路'),
  (4, '综合办公楼路口', '图书馆西侧路口', 'walk', 1.00, 0, '综合办公楼到图书馆西侧'),
  (4, '风味餐厅路口', '鸿雁路国脉路口', 'walk', 1.00, 0, '风味餐厅路口到鸿雁路'),
  (4, '学生活动中心路口', '甲子钟路口', 'walk', 1.00, 0, '学生活动中心路口到鸿雁路'),
  (4, '风味餐厅路口', '学生活动中心路口', 'walk', 1.00, 0, '生活服务区中部横路'),
  (4, '鸿雁路国脉路口', '公共教学楼西路口', 'walk', 1.00, 0, '国脉路向南到公共教学楼'),
  (4, '公共教学楼西路口', '南区生活路口', 'walk', 1.00, 0, '公共教学楼到南区生活区'),
  (4, '公共教学楼西路口', '下沉广场路口', 'walk', 1.00, 0, '公共教学楼东侧到下沉广场'),
  (4, '西校门', '西校门内侧路口', 'walk', 1.00, 0, '西校门进入校园'),
  (4, '南校门', '南门内侧路口', 'walk', 1.00, 0, '南校门进入校园'),
  (4, '东校门', '东校门内侧路口', 'walk', 1.00, 0, '东校门进入校园'),
  (4, '北校门', '北门内侧路口', 'walk', 1.00, 0, '北校门进入校园'),
  (4, '体育馆（在建）', '体育馆路口', 'walk', 1.00, 0, '体育馆接入西侧道路'),
  (4, '体育馆路口', '西校门内侧路口', 'walk', 1.00, 0, '体育馆路口到西校门内侧路口'),
  (4, '西体育场', '鸿雁路西段路口', 'walk', 1.00, 0, '西体育场接入鸿雁路'),
  (4, '东体育场', '南侧体育路口', 'walk', 1.00, 0, '东体育场接入南侧体育路口'),
  (4, '沙河校区图书馆', '图书馆西侧路口', 'walk', 1.00, 0, '图书馆西侧入口'),
  (4, '沙河校区图书馆', '图书馆东侧路口', 'walk', 1.00, 0, '图书馆东侧入口'),
  (4, '甲子钟广场', '甲子钟路口', 'walk', 1.00, 0, '甲子钟广场接入鸿雁路'),
  (4, '北区食堂', '北区生活路口', 'walk', 1.00, 0, '北区食堂接入生活区道路'),
  (4, '风味餐厅', '风味餐厅路口', 'walk', 1.00, 0, '风味餐厅接入道路'),
  (4, '教工餐厅', '学生活动中心路口', 'walk', 1.00, 0, '教工餐厅接入活动中心路口'),
  (4, '南区食堂', '南区生活路口', 'walk', 1.00, 0, '南区食堂接入南区生活路口'),
  (4, '学生活动中心', '学生活动中心路口', 'walk', 1.00, 0, '学生活动中心接入道路'),
  (4, '师生综合服务大厅', '学生活动中心路口', 'walk', 1.00, 0, '服务大厅接入活动中心路口'),
  (4, '综合办公楼', '综合办公楼路口', 'walk', 1.00, 0, '综合办公楼接入道路'),
  (4, '公共教学楼', '公共教学楼西路口', 'walk', 1.00, 0, '公共教学楼接入道路'),
  (4, '校医院', '校医院路口', 'walk', 1.00, 0, '校医院接入道路'),
  (4, '快递中心', '西校门内侧路口', 'walk', 1.00, 0, '快递中心接入西门主路'),
  (4, '校园超市', '学生活动中心路口', 'walk', 1.00, 0, '校园超市接入活动中心路口'),
  (4, '咖啡店', '综合办公楼路口', 'walk', 1.00, 0, '咖啡店接入综合办公楼路口'),
  (4, '打印店', '学生活动中心路口', 'walk', 1.00, 0, '打印店接入活动中心路口'),
  (4, '理发店', '校医院路口', 'walk', 1.00, 0, '理发店接入校医院路口'),
  (4, '洗衣房', '校医院路口', 'walk', 1.00, 0, '洗衣房接入校医院路口'),
  (4, '雁北园学生公寓', '雁北园路口', 'walk', 1.00, 0, '雁北园接入道路'),
  (4, '雁南园学生公寓', '南区生活路口', 'walk', 1.00, 0, '雁南园接入南区生活路口'),
  (4, '留学生公寓', '北区生活路口', 'walk', 1.00, 0, '留学生公寓接入北区生活路口'),
  (4, '教学实验综合楼 N 楼', '教学实验楼路口', 'walk', 1.00, 0, '教学实验综合楼 N 楼接入道路'),
  (4, '教学实验综合楼 S 楼', '教学实验楼路口', 'walk', 1.00, 0, '教学实验综合楼 S 楼接入道路'),
  (4, '智慧教学楼', '智慧教学楼路口', 'walk', 1.00, 0, '智慧教学楼接入道路'),
  (4, '网络空间安全学院楼', '图书馆东侧路口', 'walk', 1.00, 0, '网安学院楼接入图书馆东侧路口'),
  (4, '数字媒体与设计艺术学院楼', '智慧教学楼路口', 'walk', 1.00, 0, '数媒学院楼接入智慧教学楼路口'),
  (4, '工程训练中心', '南侧体育路口', 'walk', 1.00, 0, '工程训练中心接入南侧体育路口'),
  (4, '中心绿地', '甲子钟路口', 'walk', 1.00, 0, '中心绿地接入甲子钟路口'),
  (4, '景观湖', '南侧体育路口', 'walk', 1.00, 0, '景观湖接入南侧体育路口'),
  (4, '友谊林', '教学区北侧路口', 'walk', 1.00, 0, '友谊林接入教学区北侧路口'),
  (4, '下沉广场', '下沉广场路口', 'walk', 1.00, 0, '下沉广场接入道路'),
  (4, '教学实验综合楼 N 楼', '教学实验综合楼 S 楼', 'indoor', 1.00, 1, '教学实验综合楼 N 楼到 S 楼室内连通');

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
FROM tmp_bupt_edges e
JOIN pois fp ON fp.place_group_id = e.place_group_id AND fp.name = e.from_name
JOIN pois tp ON tp.place_group_id = e.place_group_id AND tp.name = e.to_name;

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  tp.id,
  fp.id,
  edge.distance_m,
  edge.duration_min,
  edge.transport_type,
  edge.congestion_factor,
  edge.is_indoor,
  CONCAT('返回：', edge.description)
FROM route_edges edge
JOIN pois fp ON fp.id = edge.from_poi_id
JOIN pois tp ON tp.id = edge.to_poi_id
WHERE fp.place_group_id IN (3, 4)
  AND tp.place_group_id IN (3, 4)
  AND NOT EXISTS (
    SELECT 1
    FROM route_edges reverse_edge
    WHERE reverse_edge.from_poi_id = tp.id
      AND reverse_edge.to_poi_id = fp.id
  );

DROP TEMPORARY TABLE tmp_bupt_edges;
DROP TEMPORARY TABLE tmp_bupt_pois;

COMMIT;

SELECT
  pg.id AS place_group_id,
  pg.name,
  pg.route_graph_status,
  SUM(CASE WHEN COALESCE(p.area_code, '') != 'route' THEN 1 ELSE 0 END) AS visible_poi_count,
  SUM(CASE WHEN p.area_code = 'route' THEN 1 ELSE 0 END) AS route_node_count
FROM place_groups pg
LEFT JOIN pois p ON p.place_group_id = pg.id
WHERE pg.id IN (3, 4)
GROUP BY pg.id, pg.name, pg.route_graph_status;

SELECT
  fp.place_group_id,
  COUNT(*) AS route_edge_count
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id IN (3, 4)
  AND tp.place_group_id IN (3, 4)
GROUP BY fp.place_group_id;
