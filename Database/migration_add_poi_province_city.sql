-- 给 POI 增加结构化省市字段
-- 用途：
-- - 景点查询按 province/city 精确过滤，不再完全依赖地址模糊匹配。
-- - 现有数据会尽量批量回填，后续新增 POI 应显式写 province/city。

USE tourist_system;

SET @has_province := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'province'
);

SET @sql := IF(
  @has_province = 0,
  'ALTER TABLE pois ADD COLUMN province VARCHAR(50) DEFAULT NULL COMMENT ''省份/直辖市/自治区'' AFTER area_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_city := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pois'
    AND COLUMN_NAME = 'city'
);

SET @sql := IF(
  @has_city = 0,
  'ALTER TABLE pois ADD COLUMN city VARCHAR(50) DEFAULT NULL COMMENT ''城市'' AFTER province',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 先从空间分组回填校区、商圈、公园等正式数据。
UPDATE pois p
JOIN place_groups pg ON pg.id = p.place_group_id
SET
  p.province = CASE
    WHEN pg.city IN ('北京', '上海', '天津', '重庆') THEN pg.city
    WHEN pg.city IS NOT NULL THEN pg.city
    ELSE p.province
  END,
  p.city = COALESCE(pg.city, p.city)
WHERE p.place_group_id IS NOT NULL
  AND p.place_group_id <> 100;

-- 全国热门占位数据没有省市字段，这里按名称做第一版批量回填。
UPDATE pois
SET province = CASE
    WHEN name REGEXP '北京|清华|故宫|颐和园|八达岭|天坛|圆明园|恭王府|中央财经|对外经济贸易|北京外国语|中国人民公安|中国传媒|北京交通|北京理工|北京师范|北京航空航天|北京邮电|华北电力' THEN '北京'
    WHEN name REGEXP '上海|复旦|同济|东方明珠|外滩|迪士尼|南京东路|华东师范|上海财经|上海科技|上海交通' THEN '上海'
    WHEN name REGEXP '天津|南开' THEN '天津'
    WHEN name REGEXP '重庆|武隆' THEN '重庆'
    WHEN name REGEXP '浙江|西湖|乌镇|千岛湖|普陀山' THEN '浙江'
    WHEN name REGEXP '江苏|南京|苏州|东南大学|中国矿业大学|南京航空航天|南京理工' THEN '江苏'
    WHEN name REGEXP '福建|厦门|鼓浪屿|武夷山|土楼' THEN '福建'
    WHEN name REGEXP '四川|成都|九寨沟|峨眉|乐山|稻城|杜甫草堂' THEN '四川'
    WHEN name REGEXP '陕西|西安|兵马俑|华山|大雁塔|西北工业|西安交通|西安电子' THEN '陕西'
    WHEN name REGEXP '湖北|武汉|华中|黄鹤楼|武昌起义|神农架|三峡大坝' THEN '湖北'
    WHEN name REGEXP '湖南|张家界|岳阳楼|凤凰|芙蓉镇|中南大学|湖南大学' THEN '湖南'
    WHEN name REGEXP '广东|广州|深圳|中山大学|华南|暨南|南方科技' THEN '广东'
    WHEN name REGEXP '安徽|黄山|宏村' THEN '安徽'
    WHEN name REGEXP '山东|青岛|泰山|山东大学|中国海洋|栈桥' THEN '山东'
    WHEN name REGEXP '河南|郑州|龙门|嵩山|少林' THEN '河南'
    WHEN name REGEXP '河北|承德|北戴河|避暑山庄' THEN '河北'
    WHEN name REGEXP '山西|云冈|平遥|五台山|乔家大院' THEN '山西'
    WHEN name REGEXP '江西|庐山|景德镇|井冈山|南昌|三清山|婺源' THEN '江西'
    WHEN name REGEXP '云南|丽江|大理|腾冲|元阳|西双版纳|玉龙|香格里拉|云南大学' THEN '云南'
    WHEN name REGEXP '贵州|黄果树|梵净山|赤水|贵州大学' THEN '贵州'
    WHEN name REGEXP '甘肃|敦煌|莫高窟|嘉峪关|兰州大学|麦积山|张掖' THEN '甘肃'
    WHEN name REGEXP '青海|青海湖|茶卡' THEN '青海'
    WHEN name REGEXP '宁夏|沙坡头|西夏王陵' THEN '宁夏'
    WHEN name REGEXP '新疆|天山|吐鲁番|喀纳斯|新疆大学' THEN '新疆'
    WHEN name REGEXP '西藏|布达拉|西藏大学' THEN '西藏'
    WHEN name REGEXP '内蒙古|呼伦贝尔|昭君墓|内蒙古大学|满洲里' THEN '内蒙古'
    WHEN name REGEXP '辽宁|沈阳|大连|东北大学|辽宁大学|大连海事' THEN '辽宁'
    WHEN name REGEXP '吉林|长白山|吉林大学|延边|查干湖|长影|松花湖' THEN '吉林'
    WHEN name REGEXP '黑龙江|哈尔滨|太阳岛|五大连池|扎龙|镜泊湖|东北林业|冰雪大世界|中央大街' THEN '黑龙江'
    WHEN name REGEXP '海南|三亚|亚龙湾|海南大学' THEN '海南'
    WHEN name REGEXP '广西|桂林|阳朔|北海|广西大学' THEN '广西'
    ELSE province
  END,
  city = CASE
    WHEN name REGEXP '北京|清华|故宫|颐和园|八达岭|天坛|圆明园|恭王府|中央财经|对外经济贸易|北京外国语|中国人民公安|中国传媒|北京交通|北京理工|北京师范|北京航空航天|北京邮电|华北电力' THEN '北京'
    WHEN name REGEXP '上海|复旦|同济|东方明珠|外滩|迪士尼|南京东路|华东师范|上海财经|上海科技|上海交通' THEN '上海'
    WHEN name REGEXP '天津|南开' THEN '天津'
    WHEN name REGEXP '重庆|武隆' THEN '重庆'
    WHEN name REGEXP '杭州|西湖|浙江大学|千岛湖' THEN '杭州'
    WHEN name REGEXP '乌镇' THEN '嘉兴'
    WHEN name REGEXP '普陀山' THEN '舟山'
    WHEN name REGEXP '南京|东南大学|南京航空航天|南京理工|夫子庙' THEN '南京'
    WHEN name REGEXP '苏州|拙政园|苏州大学' THEN '苏州'
    WHEN name REGEXP '中国矿业大学' THEN '徐州'
    WHEN name REGEXP '厦门|鼓浪屿|厦门大学' THEN '厦门'
    WHEN name REGEXP '武夷山|土楼' THEN '南平'
    WHEN name REGEXP '成都|大熊猫|四川大学|杜甫草堂' THEN '成都'
    WHEN name REGEXP '九寨沟' THEN '阿坝'
    WHEN name REGEXP '峨眉|乐山' THEN '乐山'
    WHEN name REGEXP '稻城' THEN '甘孜'
    WHEN name REGEXP '西安|兵马俑|大雁塔|西安交通|西北工业|西安电子' THEN '西安'
    WHEN name REGEXP '华山' THEN '渭南'
    WHEN name REGEXP '武汉|华中|黄鹤楼|武昌起义|武汉大学' THEN '武汉'
    WHEN name REGEXP '张家界' THEN '张家界'
    WHEN name REGEXP '岳阳楼' THEN '岳阳'
    WHEN name REGEXP '凤凰|芙蓉镇' THEN '湘西'
    WHEN name REGEXP '湖南大学|中南大学' THEN '长沙'
    WHEN name REGEXP '广州|广州塔|中山大学|华南|暨南' THEN '广州'
    WHEN name REGEXP '深圳|南方科技' THEN '深圳'
    WHEN name REGEXP '黄山|宏村' THEN '黄山'
    WHEN name REGEXP '青岛|栈桥|中国海洋' THEN '青岛'
    WHEN name REGEXP '泰山|山东大学' THEN '济南'
    WHEN name REGEXP '郑州大学' THEN '郑州'
    WHEN name REGEXP '龙门|少林|嵩山' THEN '洛阳'
    ELSE city
  END
WHERE place_group_id = 100;

SELECT
  COUNT(*) AS total_pois,
  SUM(province IS NOT NULL) AS province_filled,
  SUM(city IS NOT NULL) AS city_filled
FROM pois;
