# 数据库设计说明

## 1. 设计目标

本数据库服务于“校园 + 城市混合的个性化旅游系统”。系统既要支持校园设施查询、楼宇/校园路线规划、食堂美食推荐，也要支持城市景点、游记社区、圈子交流和个性化推荐。

第一版数据库设计目标：

1. 支撑当前前端接口契约中的全部核心页面。
2. 让后端可以基于 MySQL 直接实现 Spring Boot 接口。
3. 突出“社区内容生态驱动个性化推荐”的项目创新点。
4. 为数据结构课设保留可讲解的图模型和推荐排序模型。

## 2. 核心设计思路

### 2.1 景点主表、内部 POI、路线节点分层建模

当前模型已经从“所有地点统一塞进 `pois`”调整为三层：

- `spots`：景点主表，表示浙江大学、上海外滩、北京邮电大学沙河校区这类“大地点”。
- `pois`：景点内部 POI，表示图书馆、食堂、教学楼、广场、校门等用户可访问点位。
- `route_nodes`：路线算法节点，表示路口、建筑入口、转弯点等图算法节点。

旧版 `place_groups` 继续保留，用来兼容学校/校区的空间层级；`spots.legacy_place_group_id` 会记录它来源于哪个空间分组。旧版全国热门景点曾经存放在 `pois` 中，现在通过 `spots.legacy_poi_id` 迁移到景点主表。

`pois` 增加了：

- `spot_id`：指向所属景点。
- `poi_role`：区分 `internal` 内部 POI、`spot_legacy` 旧景点行、`route_legacy` 旧路线节点。

这样可以避免“浙江大学”和“甲子钟路口”在同一层语义里混用。旧字段保留是为了让现有后端接口逐步迁移，不在一次 SQL 重构里直接破坏运行路径。

### 2.2 社区内容生态

项目创新点不是简单推荐热门景点，而是让用户行为和社区内容沉淀为推荐依据。

相关表包括：

- `travel_logs`：用户游记和圈子日志。
- `circles`：兴趣圈子。
- `circle_members`：圈子成员关系。
- `log_likes`：日志点赞。
- `log_comments`：日志评论和回复。
- `favorites`：收藏行为。
- `browsing_history`：浏览行为。
- `ratings`：评分行为。
- `user_preferences`：用户兴趣画像。

这些表可以回答：

- 用户喜欢哪些类型的 POI？
- 哪些游记、圈子、景点正在被讨论？
- 某个景点是否因为社区内容活跃而值得推荐？
- 用户加入的圈子能否反映他的旅行兴趣？

### 2.3 推荐逻辑支撑

第一版推荐可以不做复杂机器学习，而是采用可解释的加权评分：

```text
推荐分数 =
  标签匹配分 * 0.30
  + 社区热度分 * 0.25
  + 用户行为分 * 0.25
  + 评分质量分 * 0.10
  + 距离便利分 * 0.10
```

各部分来源：

- 标签匹配分：`user_preferences`、`tags`、`poi_tag_relations`
- 社区热度分：`travel_logs`、`log_likes`、`log_comments`
- 用户行为分：`favorites`、`browsing_history`、`ratings`
- 评分质量分：`pois.rating`、`foods.rating`
- 距离便利分：`pois.longitude`、`pois.latitude`，由后端临时计算

### 2.4 路线图模型

路线规划用图结构表示：

- `route_nodes` 是图中的节点。
- `route_node_edges` 是图中的边。
- `distance_m`、`duration_min`、`congestion_factor` 是边权重。

后端可以基于这些数据实现：

- Dijkstra 最短路径。
- 多点路线规划。
- 校园室内路径筛选。

旧版 `route_edges` 仍然保留作为兼容表。`migration_refactor_spot_poi_route_schema.sql` 已经把旧 `route_edges` 的端点迁移到 `route_nodes`，并生成 `route_node_edges`。后端路线模块后续应切换到新表。

`route_records` 和 `route_record_points` 用于保存一次路线规划结果，方便后续展示历史路线或调试算法输出。

## 3. 表结构分组

### 3.1 用户模块

- `users`：账号、手机号、密码哈希、登录状态。
- `user_profiles`：昵称、头像、签名、邮箱、性别、生日、访问数。
- `user_preferences`：用户兴趣画像。

### 3.2 景点、POI 与标签模块

- `spots`：景点主表，可表示高校、校区、景区、公园、商圈等。
- `place_groups`：空间分组，可表示高校、校区、景区、公园、商圈等。
- `poi_categories`：教学楼、图书馆、食堂、景观、博物馆、自然风景、历史古迹、主题乐园等分类。
- `pois`：景点内部 POI 和旧数据兼容表。
- `tags`：兴趣标签、内容标签、POI 标签。
- `spot_tag_relations`：景点与标签多对多关系。
- `poi_tag_relations`：POI 与标签多对多关系。

### 3.3 行为数据模块

- `favorites`：收藏 POI、美食或日志。
- `browsing_history`：浏览记录。
- `ratings`：评分记录。

这些数据是个性化推荐的重要输入。

### 3.4 美食模块

- `foods`：美食基础信息。
- `food_reviews`：美食评价。

美食通过 `foods.poi_id` 关联到食堂、景区、商圈等 POI。

### 3.5 路线模块

- `route_edges`：路线图的边。
- `route_nodes`：新路线节点表。
- `route_node_edges`：新路线边表。
- `route_records`：路线规划记录。
- `route_record_points`：路线中的点位顺序。

### 3.6 社区模块

- `circles`：兴趣圈子。
- `circle_members`：成员和角色。
- `travel_logs`：个人游记和圈子日志。
- `log_images`：日志图片。
- `log_tag_relations`：日志标签。
- `log_likes`：日志点赞。
- `log_comments`：日志评论和回复。

## 4. 字段落库边界

前端字段不全部机械落库，需要区分来源：

### 必须落库

- 用户账号、资料、偏好。
- POI、分类、标签、坐标、开放时间。
- 空间分组，例如高校、校区、景区、公园、商圈。
- 美食、评价。
- 游记、图片、评论、点赞。
- 圈子和成员。
- 路线节点和边。
- 浏览、收藏、评分行为。

### SQL 聚合得到

- 圈子成员数：由 `circle_members` 聚合。
- 圈子日志数：由 `travel_logs.circle_id` 聚合。
- 日志点赞数：由 `log_likes` 聚合。
- 日志评论数：由 `log_comments` 聚合。
- 用户收藏数：由 `favorites` 聚合。
- 平均评分：可由 `ratings` 或 `food_reviews` 聚合后回写。

### 后端临时计算

- 距离 `distance`
- 推荐分数
- 推荐理由
- 路线总距离、路线总时长
- 是否已收藏、是否已点赞

### 前端占位

- 图片文件真实上传。
- 分享链接。
- 真实地图 SDK 绘制。
- 忘记密码流程。

## 5. 接口适配说明

数据库可以支撑当前前端的主要接口：

- `/user/*`：由 `users`、`user_profiles` 支撑。
- `/search/*`：由 `poi_categories`、`pois`、`tags` 支撑。
- `/search/*` 可按 `place_groups` 增加学校、校区、景区、商圈筛选。
- `/recommend/*`：由 POI、标签、偏好、行为、社区内容共同支撑。
- `/food/*`：由 `foods`、`food_reviews` 支撑。
- `/route/*`：由 `pois`、`route_edges`、`route_records` 支撑。
- `/log/*`：由 `travel_logs`、`log_images`、`log_likes`、`log_comments` 支撑。
- `/circle/*`：由 `circles`、`circle_members`、`travel_logs` 支撑。

## 6. 后续扩展

后续可以继续扩展：

- 管理员审核系统。
- 内容举报系统。
- AI 对话推荐历史。
- 更复杂的推荐权重配置表。
- 地图 SDK 路线轨迹表。
- 城市行政区、楼层、房间等更细粒度空间模型。
