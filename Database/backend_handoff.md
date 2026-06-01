# 后端数据库交接说明

本文档面向后端开发同学，说明数据库初始化顺序、核心表关系、接口实现查询方式和路线规划/推荐模块的落库边界。

## 1. 当前数据库状态

本项目数据库名：

```sql
tourist_system
```

当前已验证的数据规模如下：

| 数据项 | 数量 | 说明 |
| --- | ---: | --- |
| users | 8 | 演示用户和联调测试用户 |
| users.role | admin=1 / user=7 | `chen` 为管理员演示账号 |
| place_groups | 9 | 北邮、北航、校区、商圈、全国热门地点集合 |
| spots | 208 | 景点主表，206 条启用 |
| place_group_maps | 0 | 管理员上传的路线底图，当前尚未上传 |
| poi_categories | 26 | 校园设施 + 城市景点细分类 |
| pois | 468 | 兼容旧表，已用 `poi_role` 区分内部 POI、旧景点行、旧路线节点 |
| route_edges | 266 | 北邮沙河真实拓扑版路网 + 西土城演示路网 |
| route_nodes | 129 | 新路线节点表，从旧 `route_edges` 端点迁移 |
| route_node_edges | 266 | 新路线边表，连接 `route_nodes` |
| foods | 4 | 第一版美食演示数据 |
| circles | 3 | 圈子演示数据 |
| travel_logs | 7 | 游记/圈子内容演示数据 |
| log_comments | 5 | 评论演示数据 |
| favorites | 4 | 收藏行为 |
| browsing_history | 5 | 浏览行为 |
| ratings | 3 | 评分行为 |

当前数据库已经通过基础完整性检查：

- `pois.category_id` 没有无效引用。
- `route_edges.from_poi_id` / `route_edges.to_poi_id` 没有无效引用。
- `foods.poi_id` 没有无效引用。
- `travel_logs.poi_id` 没有无效引用。
- `favorites.user_id` 没有无效引用。
- 同一 `place_group_id + name` 下没有重复 POI。

## 2. SQL 执行顺序

第一次初始化或重建库时，按下面顺序执行：

```text
1. Database/schema.sql
2. Database/seed.sql
3. Database/campus_poi_seed.sql
4. Database/bupt_route_edges_seed.sql
5. Database/placeholder_poi_seed.sql
6. Database/migration_add_admin_role_and_route_maps.sql
7. Database/migration_add_shahe_map_coordinates.sql
8. Database/migration_refine_shahe_south_west_route.sql
9. Database/migration_add_campus_summary_pois.sql
10. Database/migration_add_poi_province_city.sql
11. Database/migration_refactor_spot_poi_route_schema.sql
12. Database/migration_add_log_dimension_ratings.sql
```

注意：

- `schema.sql` 会 `DROP TABLE` 并重建表，执行前确认本地数据可以清空。
- `campus_poi_seed.sql` 必须在 `seed.sql` 之后执行，因为它依赖基础分类和空间分组。
- `bupt_route_edges_seed.sql` 必须在 `campus_poi_seed.sql` 之后执行，因为它需要北邮西土城 POI。
- `placeholder_poi_seed.sql` 是 200 条全国热门高校/景区占位数据，用于列表、搜索、分页和推荐测试。
- `migration_add_admin_role_and_route_maps.sql` 会补充管理员角色字段和路线底图表，已导入旧库时也可以单独执行。
- `migration_add_shahe_map_coordinates.sql` 会补充 `pois.map_x/map_y` 和地图尺寸字段，并给北邮沙河校区写入第一版平面图坐标。
- `migration_refine_shahe_south_west_route.sql` 会补充沙河校区南门到西门的 L 型隐藏路口和路线边。
- `migration_add_campus_summary_pois.sql` 会新增四个校区代表 POI，用于首页 Top10 和景点简称查询。
- `migration_add_poi_province_city.sql` 会给 `pois` 增加 `province/city` 字段，并批量回填已有 POI 的省市。
- `migration_refactor_spot_poi_route_schema.sql` 会建立 `spots / route_nodes / route_node_edges`，并给旧 `pois` 增加 `spot_id / poi_role` 做兼容迁移。
- `migration_add_log_dimension_ratings.sql` 会给 `travel_logs` 增加五个可选评分字段；不带评分的日记字段保持 NULL，不参与景点均分。
- 不建议在同一个库里重复执行普通 seed 文件；如果要重新导入，优先重新执行完整初始化顺序。

## 3. 后端实现总原则

接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

字段转换原则：

| 前端字段 | 数据库来源 |
| --- | --- |
| `type` / `category` | `poi_categories.code` |
| `typeName` / `categoryName` | `poi_categories.name` |
| `location` | `pois.location_text` |
| `image` | `pois.image_url` |
| `score` / `rating` | `pois.rating` 或 `foods.rating` |
| `visits` | `pois.visit_count` |
| `hotness` | `pois.hotness` |
| `placeGroupName` | `place_groups.name` |
| `like_count` | 聚合 `log_likes` |
| `comment_count` | 聚合 `log_comments` |
| `members` | 聚合 `circle_members` |
| `posts` / `logs` | 聚合 `travel_logs` |

后端临时计算，不需要直接落库：

- `distance`
- `recommendReason`
- `recommendScore`
- `is_favorite`
- `is_liked`
- 路线规划结果中的临时 `points`

可以落库但第一版不强制：

- `route_records`
- `route_record_points`

## 4. POI 查询接口

### 4.1 GET `/search/types`

查询启用分类：

```sql
SELECT
  code AS value,
  name AS label,
  scene
FROM poi_categories
ORDER BY sort_order ASC, id ASC;
```

第一版可以全部返回；如果前端只想看城市景点或校园设施，可按 `scene IN ('city', 'both')` 或 `scene IN ('campus', 'both')` 过滤。

### 4.2 GET `/search/facilities`

建议参数：

| 参数 | 含义 |
| --- | --- |
| `keyword` | 名称/描述关键字 |
| `type` | 分类编码，对应 `poi_categories.code` |
| `scene` | `campus` 或 `city` |
| `placeGroupId` | 空间分组 ID，例如北邮沙河为 4 |
| `page` | 页码 |
| `pageSize` | 每页数量 |

基础查询：

```sql
SELECT
  p.id,
  p.name,
  c.code AS type,
  c.name AS typeName,
  c.name AS categoryName,
  p.description,
  p.location_text AS location,
  p.image_url AS image,
  p.longitude,
  p.latitude,
  p.open_time AS openTime,
  p.close_time AS closeTime,
  p.phone,
  p.rating,
  p.hotness,
  p.visit_count AS visits,
  pg.id AS placeGroupId,
  pg.name AS placeGroupName
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
LEFT JOIN place_groups pg ON pg.id = p.place_group_id
WHERE p.status = 1
  AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%'))
  AND (:type IS NULL OR c.code = :type)
  AND (:scene IS NULL OR p.scene = :scene)
  AND (:placeGroupId IS NULL OR p.place_group_id = :placeGroupId)
ORDER BY p.hotness DESC, p.rating DESC, p.id ASC
LIMIT :offset, :pageSize;
```

总数查询使用同样的 `WHERE` 条件：

```sql
SELECT COUNT(*)
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
WHERE p.status = 1
  AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%'))
  AND (:type IS NULL OR c.code = :type)
  AND (:scene IS NULL OR p.scene = :scene)
  AND (:placeGroupId IS NULL OR p.place_group_id = :placeGroupId);
```

### 4.3 GET `/search/facility/{id}`

```sql
SELECT
  p.*,
  c.code AS type,
  c.name AS typeName,
  pg.name AS placeGroupName
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
LEFT JOIN place_groups pg ON pg.id = p.place_group_id
WHERE p.id = :id
  AND p.status = 1;
```

详情页如果要展示标签：

```sql
SELECT t.name
FROM poi_tag_relations ptr
JOIN tags t ON t.id = ptr.tag_id
WHERE ptr.poi_id = :poiId
ORDER BY t.id;
```

## 5. 推荐接口

### 5.1 GET `/recommend/hot-top10`

第一版直接按热度、评分和访问量排序：

```sql
SELECT
  p.id,
  p.name,
  c.code AS category,
  c.name AS categoryName,
  p.rating AS score,
  p.hotness,
  p.visit_count AS visits,
  p.image_url AS image
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
WHERE p.status = 1
ORDER BY p.hotness DESC, p.rating DESC, p.visit_count DESC
LIMIT 10;
```

### 5.2 GET `/recommend/list`

第一版建议采用可解释加权，不做复杂机器学习。

推荐分数可以由后端临时计算：

```text
recommend_score =
  category_preference_score * 0.30
  + behavior_score * 0.25
  + community_score * 0.25
  + rating_score * 0.10
  + hotness_score * 0.10
```

简化 SQL 可先用评分、热度、社区游记数排序：

```sql
SELECT
  p.id,
  p.name,
  p.image_url AS image,
  p.description,
  c.code AS category,
  c.name AS categoryName,
  p.rating AS score,
  p.hotness,
  p.visit_count AS visits,
  COUNT(DISTINCT l.id) AS logCount,
  COUNT(DISTINCT f.id) AS favoriteCount
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
LEFT JOIN travel_logs l ON l.poi_id = p.id
LEFT JOIN favorites f ON f.target_type = 'poi' AND f.target_id = p.id
WHERE p.status = 1
GROUP BY p.id, c.code, c.name
ORDER BY
  (p.hotness * 0.4 + p.rating * 200 * 0.2 + COUNT(DISTINCT l.id) * 80 + COUNT(DISTINCT f.id) * 60) DESC
LIMIT :offset, :pageSize;
```

`recommendReason` 可以由后端根据主导因素拼：

- 用户偏好命中：`根据你的兴趣偏好推荐`
- 社区内容多：`近期社区讨论较多`
- 热度高：`当前热度较高`
- 评分高：`综合评分较高`

## 6. 路线规划接口

### 6.1 路线图数据范围

当前 `route_edges` 重点覆盖：

- 北京邮电大学沙河校区：`place_group_id = 4`
- 北京邮电大学西土城校区：`place_group_id = 3`

当前已验证路线边：

| 校区 | place_group_id | route_edges |
| --- | ---: | ---: |
| 北京邮电大学西土城校区 | 3 | 110 |
| 北京邮电大学沙河校区 | 4 | 156 |

沙河校区路线已由 `Database/route_edges_bupt_shahe_realistic.sql` 重构：新增 `area_code = 'route'` 的路口/入口型 POI 作为算法节点，并按沙河平面图重建双向边。`Database/migration_refine_shahe_south_west_route.sql` 又补充了南门到西门的 L 型主路。前端起点/终点选择器会隐藏这些路线节点，但 Dijkstra 结果会经过它们，从而避免建筑之间直接斜连或无意义绕行。

### 6.1 路线底图

路线底图保存在 `place_group_maps`：

- `place_group_id`：对应某个具体景点/校区，例如沙河校区是 `4`。
- `image_url`：OSS 图片地址。
- `uploaded_by`：上传管理员用户 ID。

接口边界：

- `GET /route/maps/{placeGroupId}`：公开读取某个景点/校区底图。
- `POST /admin/maps/{placeGroupId}`：管理员上传或覆盖底图，普通用户会返回 403。

沙河校区底图当前按 2048 x 1979 像素坐标标定。前端路线图优先使用 `mapX/mapY` 画点和线，只有没有平面图坐标时才回退到经纬度归一化投影。因此如果某个点在图上偏了，优先调整 `pois.map_x/map_y`，不要改经纬度。

### 6.2 GET `/route/pois`

建议优先返回有路线边的 POI，不要返回完全孤立的 POI：

```sql
SELECT DISTINCT
  p.id,
  p.name,
  c.name AS category,
  c.code AS type,
  p.longitude,
  p.latitude,
  p.location_text AS location,
  pg.id AS placeGroupId,
  pg.name AS placeGroupName
FROM pois p
JOIN poi_categories c ON c.id = p.category_id
LEFT JOIN place_groups pg ON pg.id = p.place_group_id
WHERE p.status = 1
  AND EXISTS (
    SELECT 1
    FROM route_edges e
    WHERE e.from_poi_id = p.id OR e.to_poi_id = p.id
  )
ORDER BY pg.sort_order ASC, c.sort_order ASC, p.hotness DESC;
```

如果前端传 `placeGroupId`，加条件：

```sql
AND p.place_group_id = :placeGroupId
```

### 6.3 POST `/route/shortest`

推荐后端支持两种入参：

```json
{
  "startPoiId": 1,
  "endPoiId": 3
}
```

兼容旧前端时，也可以接收名称或坐标，但后端内部最好先转换成 `poi_id`。

读取图边：

```sql
SELECT
  e.from_poi_id,
  e.to_poi_id,
  e.distance_m,
  e.duration_min,
  e.transport_type,
  e.congestion_factor,
  e.is_indoor,
  e.description
FROM route_edges e
JOIN pois p1 ON p1.id = e.from_poi_id
JOIN pois p2 ON p2.id = e.to_poi_id
WHERE p1.place_group_id = :placeGroupId
  AND p2.place_group_id = :placeGroupId;
```

后端构建邻接表：

```java
Map<Long, List<RouteEdge>> graph = new HashMap<>();
```

Dijkstra 权重：

```text
shortest: distance_m
time: duration_min * congestion_factor
optimal: distance_m + duration_min * 30 + congestion_factor * 100
indoor: 只允许 is_indoor = 1，或给室外边加惩罚
```

返回结构：

```json
{
  "duration": 12,
  "distance": 830,
  "points": [
    { "id": 1, "name": "沙河校区图书馆", "description": "起点", "distance": 0 },
    { "id": 2, "name": "甲子钟广场", "description": "途经点", "distance": 350 },
    { "id": 3, "name": "北区食堂", "description": "终点", "distance": 830 }
  ]
}
```

路径回溯后，需要再查 POI 名称：

```sql
SELECT id, name
FROM pois
WHERE id IN (:pathPoiIds);
```

注意保持返回点顺序，应以后端 Dijkstra 回溯出的 path 顺序为准，不要依赖 `IN` 查询顺序。

### 6.4 route_records 是否必须写入

第一版接口可以只返回临时计算结果，不强制保存路线。

如果需要“我的路线记录”，再写：

1. 插入 `route_records`。
2. 按路径顺序批量插入 `route_record_points`。

## 7. 美食接口

### 7.1 GET `/food/list`

```sql
SELECT
  f.id,
  f.name,
  f.image_url AS image,
  f.description,
  f.rating AS score,
  f.rating,
  f.price_level AS priceLevel,
  f.avg_price AS avgPrice,
  f.cuisine_code AS cuisine,
  f.cuisine_name AS cuisineName,
  f.cuisine_name AS cuisine_type,
  f.address,
  f.open_time AS openTime,
  f.close_time AS closeTime,
  f.phone,
  f.hotness,
  p.id AS spot_id,
  p.name AS spot_name
FROM foods f
JOIN pois p ON p.id = f.poi_id
WHERE (:cuisine IS NULL OR f.cuisine_code = :cuisine)
ORDER BY f.hotness DESC, f.rating DESC
LIMIT :offset, :pageSize;
```

当前美食数据较少，足够第一版页面展示，但不适合复杂筛选演示。后续可以单独补充 `food_seed.sql`。

## 8. 游记接口

### 8.1 GET `/log/list`

```sql
SELECT
  l.id,
  l.user_id,
  COALESCE(up.nickname, u.username) AS username,
  l.title,
  l.content,
  l.poi_id AS spotId,
  l.poi_id AS spot_id,
  p.name AS location,
  l.rating,
  l.hotness,
  l.view_count,
  l.is_top,
  l.created_at,
  COUNT(DISTINCT ll.user_id) AS like_count,
  COUNT(DISTINCT lc.id) AS comment_count
FROM travel_logs l
JOIN users u ON u.id = l.user_id
LEFT JOIN user_profiles up ON up.user_id = u.id
LEFT JOIN pois p ON p.id = l.poi_id
LEFT JOIN log_likes ll ON ll.log_id = l.id
LEFT JOIN log_comments lc ON lc.log_id = l.id
WHERE (:circleId IS NULL OR l.circle_id = :circleId)
  AND (:userId IS NULL OR l.user_id = :userId)
GROUP BY l.id, u.username, up.nickname, p.name
ORDER BY l.is_top DESC, l.created_at DESC
LIMIT :offset, :pageSize;
```

图片单独查：

```sql
SELECT log_id, image_url
FROM log_images
WHERE log_id IN (:logIds)
ORDER BY log_id, sort_order;
```

标签单独查：

```sql
SELECT ltr.log_id, t.name
FROM log_tag_relations ltr
JOIN tags t ON t.id = ltr.tag_id
WHERE ltr.log_id IN (:logIds)
ORDER BY ltr.log_id, t.id;
```

### 8.2 POST `/log/create`

写入顺序：

1. 插入 `travel_logs`。
2. 如果有图片，插入 `log_images`。
3. 如果有标签，插入 `log_tag_relations`。
4. 可选：插入一条 `browsing_history` 或更新用户偏好。

## 9. 圈子接口

### 9.1 GET `/circle/list`

```sql
SELECT
  c.id,
  c.name,
  c.description,
  c.cover_url AS cover,
  COUNT(DISTINCT cm.user_id) AS members,
  COUNT(DISTINCT l.id) AS posts,
  CASE WHEN my_cm.user_id IS NULL THEN 0 ELSE 1 END AS is_member
FROM circles c
LEFT JOIN circle_members cm ON cm.circle_id = c.id
LEFT JOIN travel_logs l ON l.circle_id = c.id
LEFT JOIN circle_members my_cm
  ON my_cm.circle_id = c.id
 AND my_cm.user_id = :currentUserId
WHERE c.status = 1
GROUP BY c.id, my_cm.user_id
ORDER BY members DESC, posts DESC, c.id ASC;
```

### 9.2 GET `/circle/{id}/logs`

复用 `/log/list` 的查询，增加：

```sql
WHERE l.circle_id = :circleId
```

### 9.3 POST `/circle/{logId}/comments`

写入：

```sql
INSERT INTO log_comments (log_id, user_id, parent_id, content)
VALUES (:logId, :userId, :parentId, :content);
```

`parent_id` 为空表示一级评论，不为空表示回复。

## 10. 当前短板和交接边界

数据库第一版已经完成，能支撑后端开发和前端联调。当前短板主要是演示丰富度，不是表结构阻塞：

- 美食数据只有 4 条，复杂美食推荐说服力有限。
- 游记、评论、点赞、收藏、浏览记录数量偏少。
- 路线图重点覆盖北邮双校区，北航校区和全国景点还没有路线边。
- 占位景点经纬度、热度、评分是演示数据，不是生产级真实数据。

后端第一版优先级建议：

1. `/search/types`
2. `/search/facilities`
3. `/search/facility/{id}`
4. `/route/pois`
5. `/route/shortest`
6. `/recommend/hot-top10`
7. `/recommend/list`
8. `/circle/list`
9. `/log/list`
10. `/food/list`

先把查询类接口跑通，再做用户写入、点赞、评论、收藏和路线记录保存。

## 11. 验证 SQL

后端接库前可以执行：

```sql
SELECT COUNT(*) FROM pois;
SELECT COUNT(*) FROM route_edges;
SELECT COUNT(*) FROM poi_categories;
```

检查路线图：

```sql
SELECT
  p1.place_group_id,
  pg.name AS group_name,
  COUNT(*) AS route_edge_count
FROM route_edges e
JOIN pois p1 ON e.from_poi_id = p1.id
JOIN pois p2 ON e.to_poi_id = p2.id
JOIN place_groups pg ON pg.id = p1.place_group_id
WHERE p1.place_group_id = p2.place_group_id
GROUP BY p1.place_group_id, pg.name
ORDER BY p1.place_group_id;
```

检查无效引用：

```sql
SELECT COUNT(*) AS invalid_poi_category
FROM pois p
LEFT JOIN poi_categories c ON p.category_id = c.id
WHERE c.id IS NULL;

SELECT COUNT(*) AS invalid_route_from
FROM route_edges e
LEFT JOIN pois p ON e.from_poi_id = p.id
WHERE p.id IS NULL;

SELECT COUNT(*) AS invalid_route_to
FROM route_edges e
LEFT JOIN pois p ON e.to_poi_id = p.id
WHERE p.id IS NULL;
```

检查重复 POI：

```sql
SELECT place_group_id, name, COUNT(*) AS duplicate_count
FROM pois
GROUP BY place_group_id, name
HAVING COUNT(*) > 1;
```
