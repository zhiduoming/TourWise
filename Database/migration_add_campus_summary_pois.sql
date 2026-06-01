-- 四个校区作为“景点本身”的代表 POI
-- 用途：
-- - 首页热门 Top10 前四位固定展示北邮/北航四个校区。
-- - 景点查询中按“北邮”“北航”等简称能查到校区，而不是只查到校区内部设施。
-- - 不删除已有设施 POI，只新增/修正 area_code = 'campus_summary' 的代表 POI。

USE tourist_system;

SET @university_category_id := (SELECT id FROM poi_categories WHERE code = 'university' LIMIT 1);

INSERT INTO pois
  (place_group_id, category_id, name, scene, area_code, area_name, address, location_text,
   description, longitude, latitude, rating, hotness, visit_count, status)
SELECT
  pg.id,
  @university_category_id,
  pg.name,
  'campus',
  'campus_summary',
  '高校校区',
  pg.address,
  pg.short_name,
  pg.description,
  pg.longitude,
  pg.latitude,
  CASE pg.id
    WHEN 3 THEN 4.9
    WHEN 4 THEN 4.8
    WHEN 5 THEN 4.8
    WHEN 6 THEN 4.7
    ELSE 4.6
  END,
  CASE pg.id
    WHEN 3 THEN 50000
    WHEN 4 THEN 49000
    WHEN 5 THEN 48000
    WHEN 6 THEN 47000
    ELSE 30000
  END,
  CASE pg.id
    WHEN 3 THEN 50000
    WHEN 4 THEN 49000
    WHEN 5 THEN 48000
    WHEN 6 THEN 47000
    ELSE 30000
  END,
  1
FROM place_groups pg
WHERE pg.id IN (3, 4, 5, 6)
  AND NOT EXISTS (
    SELECT 1
    FROM pois p
    WHERE p.place_group_id = pg.id
      AND p.area_code = 'campus_summary'
  );

UPDATE pois p
JOIN place_groups pg ON pg.id = p.place_group_id
SET
  p.category_id = @university_category_id,
  p.name = pg.name,
  p.scene = 'campus',
  p.area_code = 'campus_summary',
  p.area_name = '高校校区',
  p.address = pg.address,
  p.location_text = pg.short_name,
  p.description = pg.description,
  p.longitude = pg.longitude,
  p.latitude = pg.latitude,
  p.rating = CASE pg.id
    WHEN 3 THEN 4.9
    WHEN 4 THEN 4.8
    WHEN 5 THEN 4.8
    WHEN 6 THEN 4.7
    ELSE p.rating
  END,
  p.hotness = CASE pg.id
    WHEN 3 THEN 50000
    WHEN 4 THEN 49000
    WHEN 5 THEN 48000
    WHEN 6 THEN 47000
    ELSE p.hotness
  END,
  p.visit_count = CASE pg.id
    WHEN 3 THEN 50000
    WHEN 4 THEN 49000
    WHEN 5 THEN 48000
    WHEN 6 THEN 47000
    ELSE p.visit_count
  END,
  p.status = 1
WHERE pg.id IN (3, 4, 5, 6)
  AND p.area_code = 'campus_summary';

SELECT
  p.id,
  p.name,
  pg.short_name,
  p.hotness
FROM pois p
JOIN place_groups pg ON pg.id = p.place_group_id
WHERE p.area_code = 'campus_summary'
  AND p.place_group_id IN (3, 4, 5, 6)
ORDER BY p.place_group_id;
