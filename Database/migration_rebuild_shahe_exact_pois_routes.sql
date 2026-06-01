-- 北京邮电大学沙河校区 POI 与路线边精确重建
-- 依据：/Users/chen/Downloads/图片素材/北邮沙河平面图.jpg
-- 本脚本只重建 place_group_id = 4，不影响西土城和其他景点。

USE tourist_system;

START TRANSACTION;

UPDATE spots
SET representative_poi_id = NULL
WHERE legacy_place_group_id = 4;

DELETE e
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;

DELETE FROM pois
WHERE place_group_id = 4;

CREATE TEMPORARY TABLE tmp_shahe_pois (
  name VARCHAR(100) PRIMARY KEY,
  category_code VARCHAR(50) NOT NULL,
  area_code VARCHAR(50) NOT NULL,
  area_name VARCHAR(50) NOT NULL,
  map_x INT NOT NULL,
  map_y INT NOT NULL,
  visible TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_shahe_pois
  (name, category_code, area_code, area_name, map_x, map_y, visible, description)
VALUES
  -- 总览点：兼容景点详情页，不出现在路线选择框
  ('北京邮电大学沙河校区', 'university', 'campus_summary', '校区', 1024, 990, 1, '北京邮电大学沙河校区总览点'),

  -- 校门
  ('西门', 'gate', 'gate', '校门', 155, 930, 1, '沙河校区西门'),
  ('南门', 'gate', 'gate', '校门', 1185, 1510, 1, '沙河校区南门'),

  -- 食堂与商业服务
  ('北区食堂（在建）', 'cafeteria', 'living', '餐饮服务', 720, 420, 1, '沙河校区北区食堂'),
  ('风味餐厅', 'cafeteria', 'living', '餐饮服务', 900, 700, 1, '沙河校区风味餐厅'),
  ('教工餐厅', 'cafeteria', 'living', '餐饮服务', 900, 850, 1, '沙河校区教工餐厅'),
  ('学生餐厅', 'cafeteria', 'living', '餐饮服务', 900, 1605, 1, '沙河校区学生餐厅'),
  ('天猫超市', 'shop', 'service', '生活服务', 1010, 850, 1, '学生活动中心内天猫超市'),
  ('地下超市', 'shop', 'living', '生活服务', 700, 930, 1, '雁北园生活服务区地下一层超市'),
  ('快递站', 'service', 'service', '生活服务', 210, 875, 1, '沙河校区快递站'),
  ('基建修缮部', 'service', 'service', '生活服务', 215, 905, 1, '沙河校区基建修缮部'),
  ('医务室', 'medical', 'service', '医疗服务', 1090, 615, 1, '沙河校区医务室'),
  ('文创店', 'shop', 'service', '生活服务', 1120, 735, 1, '综合办公楼旁文创店'),

  -- 教学科研与学院楼
  ('公共教学楼', 'teaching', 'teaching', '教学科研', 1000, 1200, 1, '沙河校区公共教学楼'),
  ('教学实验综合楼', 'teaching', 'teaching', '教学科研', 1450, 1120, 1, '沙河校区教学实验综合楼'),
  ('报告厅', 'teaching', 'teaching', '教学科研', 1395, 1135, 1, '教学实验综合楼报告厅'),
  ('智慧教学楼', 'teaching', 'teaching', '教学科研', 1730, 1120, 1, '沙河校区智慧教学楼'),
  ('工程实验楼', 'training', 'teaching', '教学科研', 1740, 1320, 1, '沙河校区工程实验楼'),
  ('科研楼（在建）', 'teaching', 'teaching', '教学科研', 1165, 315, 1, '沙河校区科研楼'),
  ('计算机学院楼（在建）', 'college', 'teaching', '教学科研', 1695, 515, 1, '沙河校区计算机学院楼'),
  ('智能工程与自动化学院楼（在建）', 'college', 'teaching', '教学科研', 1715, 305, 1, '沙河校区智能工程与自动化学院楼'),
  ('网络空间安全学院', 'college', 'teaching', '教学科研', 1700, 760, 1, '沙河校区网络空间安全学院'),
  ('数字媒体与设计艺术学院', 'college', 'teaching', '教学科研', 1645, 920, 1, '沙河校区数字媒体与设计艺术学院'),
  ('智能工程与自动化学院', 'college', 'teaching', '教学科研', 1760, 970, 1, '沙河校区智能工程与自动化学院'),
  ('理学楼', 'college', 'teaching', '教学科研', 1710, 1510, 1, '沙河校区理学楼'),
  ('人文学院', 'college', 'teaching', '教学科研', 1850, 1120, 1, '沙河校区人文学院'),
  ('马克思主义学院', 'college', 'teaching', '教学科研', 1700, 1270, 1, '沙河校区马克思主义学院'),

  -- 学习、办公与公共空间
  ('图书馆', 'library', 'teaching', '学习空间', 1390, 830, 1, '沙河校区图书馆'),
  ('东配楼', 'office', 'office', '办公区', 1490, 760, 1, '图书馆东配楼'),
  ('综合办公楼', 'office', 'office', '办公区', 1070, 700, 1, '沙河校区综合办公楼'),
  ('学生活动中心', 'activity', 'service', '公共服务', 1070, 850, 1, '沙河校区学生活动中心'),
  ('甲子钟', 'landmark', 'scenic', '地标', 500, 925, 1, '沙河校区甲子钟'),
  ('北邮星塔', 'landmark', 'scenic', '地标', 1170, 615, 1, '沙河校区北邮星塔'),

  -- 宿舍区
  ('学生宿舍 IV 区（在建）', 'dormitory', 'living', '宿舍区', 700, 285, 1, '沙河校区学生宿舍 IV 区'),
  ('雁北 A 区', 'dormitory', 'living', '宿舍区', 650, 940, 1, '雁北园学生公寓 A 区'),
  ('雁北 B 区', 'dormitory', 'living', '宿舍区', 650, 865, 1, '雁北园学生公寓 B 区'),
  ('雁北 C 区', 'dormitory', 'living', '宿舍区', 720, 785, 1, '雁北园学生公寓 C 区'),
  ('雁北 D1 区', 'dormitory', 'living', '宿舍区', 760, 865, 1, '雁北园学生公寓 D1 区'),
  ('雁北 D2 区', 'dormitory', 'living', '宿舍区', 765, 655, 1, '雁北园学生公寓 D2 区'),
  ('雁北 E 区', 'dormitory', 'living', '宿舍区', 690, 670, 1, '雁北园学生公寓 E 区'),
  ('雁南 S1 区', 'dormitory', 'living', '宿舍区', 660, 1160, 1, '雁南园学生公寓 S1 区'),
  ('雁南 S2 区', 'dormitory', 'living', '宿舍区', 735, 1160, 1, '雁南园学生公寓 S2 区'),
  ('雁南 S3 区', 'dormitory', 'living', '宿舍区', 725, 1290, 1, '雁南园学生公寓 S3 区'),
  ('雁南 S4 区', 'dormitory', 'living', '宿舍区', 725, 1395, 1, '雁南园学生公寓 S4 区'),
  ('雁南 S5 区', 'dormitory', 'living', '宿舍区', 725, 1505, 1, '雁南园学生公寓 S5 区'),
  ('雁南 S6 区', 'dormitory', 'living', '宿舍区', 725, 1685, 1, '雁南园学生公寓 S6 区'),
  ('研究生宿舍楼（在建）', 'dormitory', 'living', '宿舍区', 470, 1580, 1, '沙河校区研究生宿舍楼'),

  -- 体育场地
  ('体育馆（在建）', 'sports', 'sports', '体育区', 310, 720, 1, '沙河校区体育馆'),
  ('运动场', 'sports', 'sports', '体育区', 420, 820, 1, '沙河校区运动场'),
  ('运动场（在建）', 'sports', 'sports', '体育区', 400, 1180, 1, '沙河校区运动场'),

  -- 隐藏路线节点
  ('西门内侧路口', 'scenic', 'route', '路线节点', 170, 925, 0, '西门内侧主路口'),
  ('甲子钟路口', 'scenic', 'route', '路线节点', 520, 925, 0, '甲子钟附近路口'),
  ('国脉西路口', 'scenic', 'route', '路线节点', 610, 925, 0, '国脉路与鸿雁路西侧路口'),
  ('鸿雁国脉路口', 'scenic', 'route', '路线节点', 815, 925, 0, '鸿雁路与国脉路路口'),
  ('鸿雁中轴路口', 'scenic', 'route', '路线节点', 1185, 925, 0, '鸿雁路与中轴路路口'),
  ('鸿雁东路口', 'scenic', 'route', '路线节点', 1545, 925, 0, '鸿雁路东侧路口'),
  ('西侧北路口', 'scenic', 'route', '路线节点', 610, 190, 0, '西侧北部道路路口'),
  ('北区食堂路口', 'scenic', 'route', '路线节点', 610, 420, 0, '北区食堂旁路口'),
  ('雁北北路口', 'scenic', 'route', '路线节点', 610, 670, 0, '雁北园北侧路口'),
  ('雁北南路口', 'scenic', 'route', '路线节点', 610, 865, 0, '雁北园南侧路口'),
  ('雁南北路口', 'scenic', 'route', '路线节点', 610, 1160, 0, '雁南园北侧路口'),
  ('雁南中路口', 'scenic', 'route', '路线节点', 610, 1395, 0, '雁南园中部路口'),
  ('雁南南路口', 'scenic', 'route', '路线节点', 610, 1685, 0, '雁南园南侧路口'),
  ('餐饮北路口', 'scenic', 'route', '路线节点', 815, 700, 0, '风味餐厅西侧路口'),
  ('餐饮南路口', 'scenic', 'route', '路线节点', 815, 850, 0, '教工餐厅西侧路口'),
  ('公共教学楼西路口', 'scenic', 'route', '路线节点', 815, 1200, 0, '公共教学楼西侧路口'),
  ('研究生宿舍路口', 'scenic', 'route', '路线节点', 610, 1580, 0, '研究生宿舍楼东侧路口'),
  ('综合办公楼路口', 'scenic', 'route', '路线节点', 1030, 700, 0, '综合办公楼西侧路口'),
  ('学生活动中心路口', 'scenic', 'route', '路线节点', 1030, 850, 0, '学生活动中心西侧路口'),
  ('中轴北路口', 'scenic', 'route', '路线节点', 1185, 190, 0, '中轴北侧路口'),
  ('科研楼路口', 'scenic', 'route', '路线节点', 1185, 420, 0, '科研楼南侧路口'),
  ('医务室路口', 'scenic', 'route', '路线节点', 1185, 615, 0, '医务室旁路口'),
  ('图书馆西路口', 'scenic', 'route', '路线节点', 1185, 830, 0, '图书馆西侧路口'),
  ('教学实验楼路口', 'scenic', 'route', '路线节点', 1185, 1120, 0, '教学实验综合楼西侧路口'),
  ('南门内侧路口', 'scenic', 'route', '路线节点', 1185, 1500, 0, '南门内侧路口'),
  ('图书馆东路口', 'scenic', 'route', '路线节点', 1545, 830, 0, '图书馆东侧路口'),
  ('学院区北路口', 'scenic', 'route', '路线节点', 1545, 590, 0, '学院区北侧路口'),
  ('教学区东路口', 'scenic', 'route', '路线节点', 1545, 1120, 0, '教学区东侧路口'),
  ('教学区南路口', 'scenic', 'route', '路线节点', 1545, 1320, 0, '教学区南侧路口'),
  ('学院东路口', 'scenic', 'route', '路线节点', 1880, 590, 0, '学院东侧北路口'),
  ('学院中路口', 'scenic', 'route', '路线节点', 1880, 925, 0, '学院东侧中路口'),
  ('学院南路口', 'scenic', 'route', '路线节点', 1880, 1320, 0, '学院东侧南路口');

INSERT INTO pois
  (spot_id, poi_role, place_group_id, category_id, name, scene, area_code, area_name,
   province, city, address, location_text, description, longitude, latitude, map_x, map_y,
   rating, hotness, visit_count, status)
SELECT
  s.id,
  CASE WHEN t.area_code = 'campus_summary' THEN 'spot_legacy'
       WHEN t.visible = 0 THEN 'route_legacy'
       ELSE 'internal' END,
  4,
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
  ROUND(116.286500 + (t.map_x - 1024) * 0.00000265, 6),
  ROUND(40.158800 - (t.map_y - 990) * 0.00000220, 6),
  t.map_x,
  t.map_y,
  4.8,
  CASE WHEN t.area_code = 'campus_summary' THEN 49000 ELSE 1000 END,
  CASE WHEN t.area_code = 'campus_summary' THEN 49000 ELSE 1000 END,
  1
FROM tmp_shahe_pois t
JOIN place_groups pg ON pg.id = 4
JOIN spots s ON s.legacy_place_group_id = 4
LEFT JOIN poi_categories c ON c.code = t.category_code;

UPDATE spots s
JOIN pois p ON p.place_group_id = 4 AND p.area_code = 'campus_summary'
SET s.representative_poi_id = p.id
WHERE s.legacy_place_group_id = 4;

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
  -- 主路骨架
  ('西门内侧路口', '甲子钟路口', 'walk', 1.00, 0, '沿鸿雁路由西门向东'),
  ('甲子钟路口', '国脉西路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('国脉西路口', '鸿雁国脉路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('鸿雁国脉路口', '鸿雁中轴路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('鸿雁中轴路口', '鸿雁东路口', 'walk', 1.00, 0, '沿鸿雁路向东'),
  ('鸿雁东路口', '学院中路口', 'walk', 1.00, 0, '沿鸿雁路向东'),

  ('西侧北路口', '北区食堂路口', 'walk', 1.00, 0, '沿国脉西侧道路向南'),
  ('北区食堂路口', '雁北北路口', 'walk', 1.00, 0, '沿国脉西侧道路向南'),
  ('雁北北路口', '雁北南路口', 'walk', 1.00, 0, '沿雁北园西侧道路向南'),
  ('雁北南路口', '国脉西路口', 'walk', 1.00, 0, '沿国脉西侧道路到鸿雁路'),
  ('国脉西路口', '雁南北路口', 'walk', 1.00, 0, '沿雁南路向南'),
  ('雁南北路口', '雁南中路口', 'walk', 1.00, 0, '沿雁南路向南'),
  ('雁南中路口', '研究生宿舍路口', 'walk', 1.00, 0, '沿雁南路向南'),
  ('研究生宿舍路口', '雁南南路口', 'walk', 1.00, 0, '沿雁南路向南'),

  ('餐饮北路口', '鸿雁国脉路口', 'walk', 1.00, 0, '餐饮区到鸿雁路'),
  ('餐饮北路口', '餐饮南路口', 'walk', 1.00, 0, '餐饮区内部道路'),
  ('餐饮南路口', '公共教学楼西路口', 'walk', 1.00, 0, '沿国脉路向南'),
  ('公共教学楼西路口', '雁南北路口', 'walk', 1.00, 0, '公共教学楼西侧到雁南园'),

  ('中轴北路口', '科研楼路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('科研楼路口', '医务室路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('医务室路口', '图书馆西路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('图书馆西路口', '鸿雁中轴路口', 'walk', 1.00, 0, '沿中轴路到鸿雁路'),
  ('鸿雁中轴路口', '教学实验楼路口', 'walk', 1.00, 0, '沿中轴路向南'),
  ('教学实验楼路口', '南门内侧路口', 'walk', 1.00, 0, '沿中轴路到南门'),

  ('学院区北路口', '图书馆东路口', 'walk', 1.00, 0, '沿东侧教学道路向南'),
  ('图书馆东路口', '鸿雁东路口', 'walk', 1.00, 0, '沿东侧教学道路到鸿雁路'),
  ('鸿雁东路口', '教学区东路口', 'walk', 1.00, 0, '沿东侧教学道路向南'),
  ('教学区东路口', '教学区南路口', 'walk', 1.00, 0, '沿东侧教学道路向南'),
  ('教学区南路口', '南门内侧路口', 'walk', 1.00, 0, '沿南侧道路到南门'),

  ('学院区北路口', '学院东路口', 'walk', 1.00, 0, '学院区北侧道路向东'),
  ('学院东路口', '学院中路口', 'walk', 1.00, 0, '学院东侧道路向南'),
  ('学院中路口', '学院南路口', 'walk', 1.00, 0, '学院东侧道路向南'),
  ('学院南路口', '教学区南路口', 'walk', 1.00, 0, '学院区南侧道路向西'),

  -- POI 接入边
  ('西门', '西门内侧路口', 'walk', 1.00, 0, '西门进入校园'),
  ('南门', '南门内侧路口', 'walk', 1.00, 0, '南门进入校园'),
  ('甲子钟', '甲子钟路口', 'walk', 1.00, 0, '甲子钟接入鸿雁路'),
  ('快递站', '甲子钟路口', 'walk', 1.00, 0, '快递站接入西侧主路'),
  ('基建修缮部', '甲子钟路口', 'walk', 1.00, 0, '基建修缮部接入西侧主路'),
  ('体育馆（在建）', '西门内侧路口', 'walk', 1.00, 0, '体育馆接入西侧主路'),
  ('运动场', '甲子钟路口', 'walk', 1.00, 0, '运动场接入甲子钟路口'),
  ('运动场（在建）', '国脉西路口', 'walk', 1.00, 0, '在建运动场接入国脉西侧道路'),

  ('学生宿舍 IV 区（在建）', '西侧北路口', 'walk', 1.00, 0, '学生宿舍 IV 区接入北侧道路'),
  ('北区食堂（在建）', '北区食堂路口', 'walk', 1.00, 0, '北区食堂接入西侧道路'),
  ('雁北 E 区', '雁北北路口', 'walk', 1.00, 0, '雁北 E 区接入雁北园道路'),
  ('雁北 D2 区', '雁北北路口', 'walk', 1.00, 0, '雁北 D2 区接入雁北园道路'),
  ('雁北 C 区', '雁北南路口', 'walk', 1.00, 0, '雁北 C 区接入雁北园道路'),
  ('雁北 B 区', '雁北南路口', 'walk', 1.00, 0, '雁北 B 区接入雁北园道路'),
  ('雁北 D1 区', '雁北南路口', 'walk', 1.00, 0, '雁北 D1 区接入雁北园道路'),
  ('雁北 A 区', '国脉西路口', 'walk', 1.00, 0, '雁北 A 区接入鸿雁路'),
  ('地下超市', '国脉西路口', 'walk', 1.00, 0, '地下超市接入雁北园生活服务区'),

  ('雁南 S1 区', '雁南北路口', 'walk', 1.00, 0, '雁南 S1 区接入雁南路'),
  ('雁南 S2 区', '雁南北路口', 'walk', 1.00, 0, '雁南 S2 区接入雁南路'),
  ('雁南 S3 区', '雁南中路口', 'walk', 1.00, 0, '雁南 S3 区接入雁南路'),
  ('雁南 S4 区', '雁南中路口', 'walk', 1.00, 0, '雁南 S4 区接入雁南路'),
  ('雁南 S5 区', '雁南南路口', 'walk', 1.00, 0, '雁南 S5 区接入雁南路'),
  ('雁南 S6 区', '雁南南路口', 'walk', 1.00, 0, '雁南 S6 区接入雁南路'),
  ('研究生宿舍楼（在建）', '研究生宿舍路口', 'walk', 1.00, 0, '研究生宿舍楼接入雁南路'),

  ('风味餐厅', '餐饮北路口', 'walk', 1.00, 0, '风味餐厅接入餐饮区道路'),
  ('教工餐厅', '餐饮南路口', 'walk', 1.00, 0, '教工餐厅接入餐饮区道路'),
  ('综合办公楼', '综合办公楼路口', 'walk', 1.00, 0, '综合办公楼接入办公区道路'),
  ('文创店', '综合办公楼路口', 'walk', 1.00, 0, '文创店接入办公区道路'),
  ('学生活动中心', '学生活动中心路口', 'walk', 1.00, 0, '学生活动中心接入服务区道路'),
  ('天猫超市', '学生活动中心路口', 'walk', 1.00, 0, '天猫超市接入学生活动中心'),
  ('综合办公楼路口', '图书馆西路口', 'walk', 1.00, 0, '综合办公楼到图书馆西侧'),
  ('学生活动中心路口', '鸿雁中轴路口', 'walk', 1.00, 0, '学生活动中心到鸿雁路'),
  ('综合办公楼路口', '学生活动中心路口', 'walk', 1.00, 0, '服务区内部道路'),

  ('科研楼（在建）', '科研楼路口', 'walk', 1.00, 0, '科研楼接入中轴路'),
  ('医务室', '医务室路口', 'walk', 1.00, 0, '医务室接入中轴路'),
  ('北邮星塔', '医务室路口', 'walk', 1.00, 0, '北邮星塔接入中轴路'),
  ('图书馆', '图书馆西路口', 'walk', 1.00, 0, '图书馆西侧接入路网'),
  ('图书馆', '图书馆东路口', 'walk', 1.00, 0, '图书馆东侧接入路网'),
  ('东配楼', '图书馆东路口', 'walk', 1.00, 0, '东配楼接入图书馆东侧路口'),
  ('公共教学楼', '公共教学楼西路口', 'walk', 1.00, 0, '公共教学楼接入国脉路'),
  ('教学实验综合楼', '教学实验楼路口', 'walk', 1.00, 0, '教学实验综合楼接入中轴路'),
  ('报告厅', '教学实验楼路口', 'walk', 1.00, 0, '报告厅接入教学实验综合楼'),
  ('学生餐厅', '南门内侧路口', 'walk', 1.00, 0, '学生餐厅接入南侧道路'),

  ('智能工程与自动化学院楼（在建）', '学院东路口', 'walk', 1.00, 0, '在建智工学院楼接入学院区道路'),
  ('计算机学院楼（在建）', '学院东路口', 'walk', 1.00, 0, '计算机学院楼接入学院区道路'),
  ('网络空间安全学院', '学院中路口', 'walk', 1.00, 0, '网络空间安全学院接入学院区道路'),
  ('数字媒体与设计艺术学院', '学院中路口', 'walk', 1.00, 0, '数媒学院接入学院区道路'),
  ('智能工程与自动化学院', '学院中路口', 'walk', 1.00, 0, '智能工程与自动化学院接入学院区道路'),
  ('智慧教学楼', '教学区东路口', 'walk', 1.00, 0, '智慧教学楼接入教学区道路'),
  ('人文学院', '教学区东路口', 'walk', 1.00, 0, '人文学院接入教学区道路'),
  ('马克思主义学院', '学院南路口', 'walk', 1.00, 0, '马克思主义学院接入学院区道路'),
  ('工程实验楼', '教学区南路口', 'walk', 1.00, 0, '工程实验楼接入教学区南侧道路'),
  ('理学楼', '学院南路口', 'walk', 1.00, 0, '理学楼接入学院区道路'),

  -- 室内/楼宇连通
  ('图书馆', '东配楼', 'indoor', 1.00, 1, '图书馆与东配楼内部连通'),
  ('教学实验综合楼', '报告厅', 'indoor', 1.00, 1, '教学实验综合楼到报告厅内部连通');

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
JOIN pois tp ON tp.place_group_id = 4 AND tp.name = e.to_name;

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
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4
  AND NOT EXISTS (
    SELECT 1
    FROM route_edges reverse_edge
    WHERE reverse_edge.from_poi_id = tp.id
      AND reverse_edge.to_poi_id = fp.id
  );

DROP TEMPORARY TABLE tmp_shahe_edges;
DROP TEMPORARY TABLE tmp_shahe_pois;

COMMIT;

SELECT
  SUM(CASE WHEN COALESCE(area_code, '') NOT IN ('route', 'campus_summary') THEN 1 ELSE 0 END) AS visible_poi_count,
  SUM(CASE WHEN area_code = 'route' THEN 1 ELSE 0 END) AS route_node_count
FROM pois
WHERE place_group_id = 4;

SELECT COUNT(*) AS route_edge_count
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = 4
  AND tp.place_group_id = 4;
