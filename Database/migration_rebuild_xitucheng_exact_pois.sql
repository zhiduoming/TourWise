-- TourWise - 北京邮电大学西土城校区 POI 重建
-- 目标：
-- 1. 备份 place_group_id = 3 的旧 POI 和相关路线边。
-- 2. 删除旧路线边和旧 POI。
-- 3. 严格按照“北邮本部平面图-可上传.jpg”上可见的命名点位重新插入用户可选 POI。
-- 4. 不自动重建 route_edges。西土城路线需要后续通过后台路线标注工具重新绘制。
--
-- 执行前确认：
-- - 当前数据库为 tourist_system。
-- - place_group_id = 3 对应“北京邮电大学西土城校区”。
-- - 本脚本会删除旧 POI；删除前会创建备份表。

USE tourist_system;

START TRANSACTION;

SET @place_group_id := 3;
SET @spot_id := (
  SELECT id
  FROM spots
  WHERE legacy_place_group_id = @place_group_id
  LIMIT 1
);

SET @fallback_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'service' LIMIT 1),
  (SELECT id FROM poi_categories ORDER BY id LIMIT 1)
);
SET @gate_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'gate' LIMIT 1), @fallback_category_id);
SET @teaching_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'teaching' LIMIT 1),
  (SELECT id FROM poi_categories WHERE code = 'university' LIMIT 1),
  @fallback_category_id
);
SET @dining_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'dining' LIMIT 1),
  (SELECT id FROM poi_categories WHERE code = 'food' LIMIT 1),
  @fallback_category_id
);
SET @dormitory_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'dormitory' LIMIT 1), @fallback_category_id);
SET @service_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'service' LIMIT 1), @fallback_category_id);
SET @sports_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'sports' LIMIT 1), @fallback_category_id);
SET @library_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'library' LIMIT 1), @teaching_category_id);
SET @medical_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'medical' LIMIT 1), @service_category_id);
SET @office_category_id := COALESCE((SELECT id FROM poi_categories WHERE code = 'office' LIMIT 1), @service_category_id);
SET @landscape_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'landscape' LIMIT 1),
  (SELECT id FROM poi_categories WHERE code = 'scenic' LIMIT 1),
  @fallback_category_id
);
SET @shopping_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'shop' LIMIT 1),
  (SELECT id FROM poi_categories WHERE code = 'shopping' LIMIT 1),
  @service_category_id
);
SET @transport_category_id := COALESCE(
  (SELECT id FROM poi_categories WHERE code = 'transport' LIMIT 1),
  @service_category_id
);

-- 备份旧数据。若同名备份表已存在，不覆盖，避免二次执行时冲掉第一次备份。
CREATE TABLE IF NOT EXISTS backup_xitucheng_pois_20260601 AS
SELECT *
FROM pois
WHERE place_group_id = @place_group_id;

CREATE TABLE IF NOT EXISTS backup_xitucheng_route_edges_20260601 AS
SELECT e.*
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = @place_group_id
   OR tp.place_group_id = @place_group_id;

-- 代表 POI 先置空，避免旧代表点被删除后还被景点详情页引用。
UPDATE spots
SET representative_poi_id = NULL,
    legacy_poi_id = NULL
WHERE legacy_place_group_id = @place_group_id;

-- 删除西土城旧路线边和旧 POI。route_edges 虽有级联，这里显式删除更容易审计。
DELETE e
FROM route_edges e
JOIN pois fp ON fp.id = e.from_poi_id
JOIN pois tp ON tp.id = e.to_poi_id
WHERE fp.place_group_id = @place_group_id
   OR tp.place_group_id = @place_group_id;

DELETE FROM pois
WHERE place_group_id = @place_group_id;

-- 插入新 POI。
-- map_x/map_y 使用原图 2491x3509 坐标系估算，后续可在后台地图标注页继续微调。
-- 经纬度按西土城校区外接矩形粗略换算，用于当前位置匹配；真实导航仍以高德大景点路线为主。
INSERT INTO pois (
  spot_id,
  poi_role,
  place_group_id,
  category_id,
  name,
  scene,
  area_code,
  area_name,
  province,
  city,
  address,
  location_text,
  description,
  image_url,
  longitude,
  latitude,
  map_x,
  map_y,
  rating,
  hotness,
  visit_count,
  status
)
SELECT
  @spot_id,
  'internal',
  @place_group_id,
  seed.category_id,
  seed.name,
  'campus',
  seed.area_code,
  seed.area_name,
  '北京市',
  '北京市',
  CONCAT('北京市海淀区西土城路10号 北京邮电大学西土城校区 ', seed.name),
  '北邮西土城校区',
  CONCAT('北邮西土城校区平面图标注点位：', seed.name),
  NULL,
  ROUND(116.354800 + seed.map_x * (0.005800 / 2491), 6),
  ROUND(39.964000 - seed.map_y * (0.004500 / 3509), 6),
  seed.map_x,
  seed.map_y,
  4.8,
  seed.hotness,
  seed.hotness,
  1
FROM (
  SELECT '北门' AS name, @gate_category_id AS category_id, 'gate' AS area_code, '校门' AS area_name, 1075 AS map_x, 370 AS map_y, 3200 AS hotness
  UNION ALL SELECT '东北门', @gate_category_id, 'gate', '校门', 2290, 620, 2800
  UNION ALL SELECT '东门', @gate_category_id, 'gate', '校门', 2290, 1450, 3000
  UNION ALL SELECT '西门', @gate_category_id, 'gate', '校门', 280, 2150, 3000
  UNION ALL SELECT '中门', @gate_category_id, 'gate', '校门', 1070, 2755, 2600
  UNION ALL SELECT '南门', @gate_category_id, 'gate', '校门', 1360, 3355, 3200

  UNION ALL SELECT '北部门锦江酒店', @service_category_id, 'service', '生活服务', 395, 575, 900
  UNION ALL SELECT '快递站', @service_category_id, 'service', '生活服务', 1270, 490, 2100
  UNION ALL SELECT '经管楼', @teaching_category_id, 'teaching', '教学科研区', 1260, 610, 2000
  UNION ALL SELECT '学十一公寓', @dormitory_category_id, 'dormitory', '宿舍区', 605, 530, 1200
  UNION ALL SELECT '学十公寓', @dormitory_category_id, 'dormitory', '宿舍区', 870, 640, 1200
  UNION ALL SELECT '学七公寓', @dormitory_category_id, 'dormitory', '宿舍区', 570, 645, 1200
  UNION ALL SELECT '学六公寓', @dormitory_category_id, 'dormitory', '宿舍区', 1530, 560, 1200
  UNION ALL SELECT '教九楼', @teaching_category_id, 'teaching', '教学科研区', 705, 710, 1500
  UNION ALL SELECT '员工宿舍', @dormitory_category_id, 'dormitory', '宿舍区', 365, 830, 900
  UNION ALL SELECT '留学生公寓', @dormitory_category_id, 'dormitory', '宿舍区', 610, 820, 1200
  UNION ALL SELECT '综合食堂', @dining_category_id, 'dining', '餐饮区', 880, 810, 2600
  UNION ALL SELECT '学五公寓', @dormitory_category_id, 'dormitory', '宿舍区', 610, 1045, 1200
  UNION ALL SELECT '学八公寓', @dormitory_category_id, 'dormitory', '宿舍区', 880, 1045, 1200
  UNION ALL SELECT '学十三公寓', @dormitory_category_id, 'dormitory', '宿舍区', 355, 1185, 1300
  UNION ALL SELECT '学三公寓', @dormitory_category_id, 'dormitory', '宿舍区', 610, 1280, 1200
  UNION ALL SELECT '学四公寓', @dormitory_category_id, 'dormitory', '宿舍区', 885, 1280, 1200
  UNION ALL SELECT '学一公寓', @dormitory_category_id, 'dormitory', '宿舍区', 610, 1540, 1200
  UNION ALL SELECT '学二公寓', @dormitory_category_id, 'dormitory', '宿舍区', 885, 1540, 1200
  UNION ALL SELECT '学29公寓', @dormitory_category_id, 'dormitory', '宿舍区', 2210, 1395, 1200

  UNION ALL SELECT '学生生活中心', @service_category_id, 'service', '生活服务', 1265, 785, 2200
  UNION ALL SELECT '综合服务楼', @service_category_id, 'service', '生活服务', 1530, 785, 2000
  UNION ALL SELECT '物美超市', @shopping_category_id, 'shopping', '购物服务', 1460, 1075, 2300
  UNION ALL SELECT '学生发展中心', @service_category_id, 'service', '生活服务', 1340, 1175, 1800
  UNION ALL SELECT '学院风味餐厅', @dining_category_id, 'dining', '餐饮区', 1555, 1105, 2400
  UNION ALL SELECT '学生风味餐厅', @dining_category_id, 'dining', '餐饮区', 1535, 1135, 2300
  UNION ALL SELECT '小松林', @landscape_category_id, 'landscape', '景观区', 1235, 1305, 1500
  UNION ALL SELECT '时光广场', @landscape_category_id, 'landscape', '景观区', 1235, 1515, 2200
  UNION ALL SELECT '图书馆', @library_category_id, 'library', '教学科研区', 1545, 1405, 4200
  UNION ALL SELECT '档案馆', @office_category_id, 'office', '办公服务', 1530, 1290, 1200
  UNION ALL SELECT '科学技术研究院', @teaching_category_id, 'teaching', '教学科研区', 1535, 1480, 1900
  UNION ALL SELECT '教四楼', @teaching_category_id, 'teaching', '教学区', 735, 1790, 2600
  UNION ALL SELECT '办公楼', @office_category_id, 'office', '办公服务', 1395, 1690, 1800
  UNION ALL SELECT '教一楼', @teaching_category_id, 'teaching', '教学区', 1390, 1825, 3300
  UNION ALL SELECT '物业中心配电室', @service_category_id, 'service', '生活服务', 1615, 1620, 700
  UNION ALL SELECT '音乐喷泉', @landscape_category_id, 'landscape', '景观区', 1340, 2150, 2100
  UNION ALL SELECT '主楼', @teaching_category_id, 'teaching', '教学区', 1570, 2160, 4200
  UNION ALL SELECT '教二楼', @teaching_category_id, 'teaching', '教学区', 1405, 2530, 3000
  UNION ALL SELECT '科学会堂', @teaching_category_id, 'teaching', '教学科研区', 1865, 2145, 2400
  UNION ALL SELECT '创新楼', @teaching_category_id, 'teaching', '教学科研区', 1870, 2520, 2400

  UNION ALL SELECT '科技成果展厅', @service_category_id, 'service', '展览服务', 1795, 820, 1300
  UNION ALL SELECT '科研楼', @teaching_category_id, 'teaching', '教学科研区', 1910, 815, 2400
  UNION ALL SELECT '工商银行 ATM', @service_category_id, 'service', '生活服务', 1910, 735, 900
  UNION ALL SELECT '校史馆', @service_category_id, 'service', '展览服务', 1865, 925, 1700
  UNION ALL SELECT '老干部活动中心', @service_category_id, 'service', '生活服务', 2150, 850, 900
  UNION ALL SELECT '学生食堂', @dining_category_id, 'dining', '餐饮区', 1840, 1080, 2500
  UNION ALL SELECT '后勤部采招办保卫处（东配楼）', @service_category_id, 'service', '后勤服务', 2190, 1060, 1100
  UNION ALL SELECT '篮球场', @sports_category_id, 'sports', '运动区', 1850, 1390, 2100
  UNION ALL SELECT '网球场排球场', @sports_category_id, 'sports', '运动区', 2030, 1390, 1800
  UNION ALL SELECT '体育馆', @sports_category_id, 'sports', '运动区', 1905, 1770, 2600
  UNION ALL SELECT '游泳馆', @sports_category_id, 'sports', '运动区', 2150, 1770, 2300
  UNION ALL SELECT '体育场', @sports_category_id, 'sports', '运动区', 2160, 2230, 3000
  UNION ALL SELECT '体育部', @office_category_id, 'office', '办公服务', 2050, 2300, 900

  UNION ALL SELECT '鸿通楼', @office_category_id, 'office', '办公服务', 365, 1850, 1200
  UNION ALL SELECT '北邮人之家邮政储蓄', @service_category_id, 'service', '生活服务', 405, 1845, 1000
  UNION ALL SELECT '基建修缮部', @service_category_id, 'service', '后勤服务', 360, 2055, 900
  UNION ALL SELECT '国际学院楼', @teaching_category_id, 'teaching', '教学区', 360, 2465, 1500
  UNION ALL SELECT '运输中心', @service_category_id, 'service', '后勤服务', 380, 2690, 900
  UNION ALL SELECT '校医院', @medical_category_id, 'medical', '医疗服务', 750, 2700, 2100
  UNION ALL SELECT '教三楼', @teaching_category_id, 'teaching', '教学区', 730, 2510, 3000
  UNION ALL SELECT '南区教学楼 A 栋', @teaching_category_id, 'teaching', '教学区', 950, 2965, 1700
  UNION ALL SELECT '南区教学楼 B 栋', @teaching_category_id, 'teaching', '教学区', 910, 3060, 1700
  UNION ALL SELECT '南区教学楼 C 栋', @teaching_category_id, 'teaching', '教学区', 795, 2965, 1700
  UNION ALL SELECT '南区教学楼 D 栋', @teaching_category_id, 'teaching', '教学区', 805, 3060, 1700
  UNION ALL SELECT '中门邮局', @service_category_id, 'service', '生活服务', 1165, 2745, 900
  UNION ALL SELECT '全民健身', @sports_category_id, 'sports', '运动区', 1475, 2920, 1700
  UNION ALL SELECT '北部幼儿园', @service_category_id, 'service', '生活服务', 1855, 3025, 900
  UNION ALL SELECT '明光楼', @teaching_category_id, 'teaching', '教学区', 2210, 3190, 1500

  UNION ALL SELECT '明光桥北车站', @transport_category_id, 'transport', '校外交通', 120, 3300, 1000
  UNION ALL SELECT '杏坛路车站', @transport_category_id, 'transport', '校外交通', 2390, 1520, 1000
  UNION ALL SELECT '杏坛路南口车站', @transport_category_id, 'transport', '校外交通', 2400, 3050, 1000
  UNION ALL SELECT '北京邮电大学南门车站', @transport_category_id, 'transport', '校外交通', 1510, 3370, 1300
) seed;

-- 选择图上明确存在且具有代表性的“主楼”作为西土城校区景点代表 POI。
UPDATE spots s
JOIN pois p ON p.place_group_id = @place_group_id AND p.name = '主楼'
SET s.representative_poi_id = p.id,
    s.legacy_poi_id = p.id
WHERE s.legacy_place_group_id = @place_group_id;

-- POI 已重建，但路线边已清空，等待后台重新标注，所以内部路网状态不能标成 verified。
UPDATE place_groups
SET route_graph_status = 'draft'
WHERE id = @place_group_id;

COMMIT;

-- 执行后验收：
-- SELECT id, name, area_code, map_x, map_y FROM pois WHERE place_group_id = 3 ORDER BY area_code, id;
-- SELECT COUNT(*) FROM route_edges e JOIN pois fp ON fp.id = e.from_poi_id WHERE fp.place_group_id = 3;
