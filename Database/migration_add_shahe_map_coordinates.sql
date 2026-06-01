-- 沙河校区平面图坐标迁移
-- 坐标系说明：
-- - 对应用户上传的「北京邮电大学沙河校区示意图」原图像素坐标。
-- - 当前图片尺寸为 2048 x 1979。
-- - map_x / map_y 只用于前端地图叠线展示，路线算法仍然使用 route_edges。

USE tourist_system;

SET @has_map_x := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'map_x'
);

SET @sql := IF(
  @has_map_x = 0,
  'ALTER TABLE pois ADD COLUMN map_x INT DEFAULT NULL COMMENT ''平面图X坐标'' AFTER latitude',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_map_y := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'map_y'
);

SET @sql := IF(
  @has_map_y = 0,
  'ALTER TABLE pois ADD COLUMN map_y INT DEFAULT NULL COMMENT ''平面图Y坐标'' AFTER map_x',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_map_width := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'place_group_maps'
    AND COLUMN_NAME = 'map_width'
);

SET @sql := IF(
  @has_map_width = 0,
  'ALTER TABLE place_group_maps ADD COLUMN map_width INT DEFAULT NULL COMMENT ''地图图片像素宽度'' AFTER original_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_map_height := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'place_group_maps'
    AND COLUMN_NAME = 'map_height'
);

SET @sql := IF(
  @has_map_height = 0,
  'ALTER TABLE place_group_maps ADD COLUMN map_height INT DEFAULT NULL COMMENT ''地图图片像素高度'' AFTER map_width',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE place_group_maps
SET map_width = 2048,
    map_height = 1979
WHERE place_group_id = 4
  AND (map_width IS NULL OR map_height IS NULL);

UPDATE pois p
JOIN (
  SELECT '西校门' AS name, 155 AS map_x, 930 AS map_y UNION ALL
  SELECT '南校门', 1185, 1510 UNION ALL
  SELECT '东校门', 1885, 920 UNION ALL
  SELECT '北门', 1185, 185 UNION ALL

  SELECT '沙河校区图书馆', 1390, 760 UNION ALL
  SELECT '甲子钟广场', 1110, 920 UNION ALL
  SELECT '中心绿地', 1185, 790 UNION ALL
  SELECT '景观湖', 1395, 1245 UNION ALL
  SELECT '友谊林', 1420, 435 UNION ALL

  SELECT '校医院', 555, 760 UNION ALL
  SELECT '公共教学楼 S1 楼', 845, 1125 UNION ALL
  SELECT '公共教学楼 S2 楼', 785, 1045 UNION ALL
  SELECT '公共教学楼 S3 楼', 785, 1195 UNION ALL
  SELECT '教学实验综合楼 N 楼', 1515, 1045 UNION ALL
  SELECT '教学实验综合楼 S 楼', 1515, 1128 UNION ALL
  SELECT '智慧教学楼', 1740, 1035 UNION ALL
  SELECT '综合办公楼', 1070, 665 UNION ALL
  SELECT '网络空间安全学院楼', 1685, 700 UNION ALL
  SELECT '数字媒体与设计艺术学院楼', 1695, 900 UNION ALL
  SELECT '工程训练中心', 1740, 1198 UNION ALL

  SELECT '西体育场', 430, 1045 UNION ALL
  SELECT '东体育场', 1785, 1180 UNION ALL
  SELECT '体育馆（在建）', 310, 720 UNION ALL
  SELECT '篮球场', 1790, 1230 UNION ALL
  SELECT '排球场', 1790, 1270 UNION ALL
  SELECT '网球场', 1815, 1315 UNION ALL
  SELECT '羽毛球场', 1845, 1335 UNION ALL
  SELECT '乒乓球场', 1785, 1115 UNION ALL
  SELECT '健身房', 1720, 1280 UNION ALL

  SELECT '北区食堂', 720, 365 UNION ALL
  SELECT '雁北园学生公寓', 640, 675 UNION ALL
  SELECT '雁南园学生公寓', 635, 1110 UNION ALL
  SELECT '南区食堂', 720, 1265 UNION ALL
  SELECT '风味餐厅', 900, 675 UNION ALL
  SELECT '教工餐厅', 900, 825 UNION ALL
  SELECT '学生活动中心', 1065, 835 UNION ALL
  SELECT '师生综合服务大厅', 1000, 890 UNION ALL
  SELECT '下沉广场', 1415, 1125 UNION ALL
  SELECT '快递中心', 175, 870 UNION ALL
  SELECT '校园超市', 1000, 795 UNION ALL
  SELECT '咖啡店', 1035, 720 UNION ALL
  SELECT '洗衣房', 585, 805 UNION ALL
  SELECT '理发店', 585, 790 UNION ALL
  SELECT '打印店', 1020, 825 UNION ALL
  SELECT '留学生公寓', 610, 330 UNION ALL
  SELECT '共享自习室', 1380, 735 UNION ALL
  SELECT '共享研讨室', 1180, 650 UNION ALL

  SELECT '西校门内侧路口', 170, 925 UNION ALL
  SELECT '校医院路口', 560, 925 UNION ALL
  SELECT '鸿雁路西段路口', 680, 925 UNION ALL
  SELECT '鸿雁路国脉路口', 815, 925 UNION ALL
  SELECT '甲子钟路口', 1040, 925 UNION ALL
  SELECT '图书馆西侧路口', 1210, 735 UNION ALL
  SELECT '图书馆南侧路口', 1215, 925 UNION ALL
  SELECT '图书馆东侧路口', 1545, 740 UNION ALL
  SELECT '智慧教学楼路口', 1595, 925 UNION ALL
  SELECT '东侧学院路口', 1870, 590 UNION ALL
  SELECT '东校门内侧路口', 1880, 925 UNION ALL
  SELECT '北门内侧路口', 1185, 190 UNION ALL
  SELECT '雁北园路口', 610, 590 UNION ALL
  SELECT '北区生活路口', 620, 410 UNION ALL
  SELECT '教学区北侧路口', 1185, 580 UNION ALL
  SELECT '教学区南侧路口', 1185, 925 UNION ALL
  SELECT '风味餐厅路口', 820, 675 UNION ALL
  SELECT '南区生活路口', 760, 1220 UNION ALL
  SELECT '学生活动中心路口', 1030, 835 UNION ALL
  SELECT '下沉广场路口', 1265, 1125 UNION ALL
  SELECT '南门内侧路口', 1185, 1500 UNION ALL
  SELECT '工程训练中心路口', 1740, 1205 UNION ALL
  SELECT '东体育场路口', 1785, 1160 UNION ALL
  SELECT '南侧体育路口', 1545, 1230 UNION ALL
  SELECT '西侧运动区路口', 590, 925
) c ON c.name = p.name
SET p.map_x = c.map_x,
    p.map_y = c.map_y
WHERE p.place_group_id = 4;

SELECT COUNT(*) AS shahe_pois_with_map_coordinates
FROM pois
WHERE place_group_id = 4
  AND map_x IS NOT NULL
  AND map_y IS NOT NULL;
