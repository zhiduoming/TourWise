# 接口与数据库映射

本文档说明当前前端接口应主要查询或写入哪些数据库表，方便后端实现。

后端正式接库时，建议同时阅读 `Database/backend_handoff.md`。该文档补充了 SQL 执行顺序、当前数据规模、接口查询 SQL、路线规划 Dijkstra 取图方式和验证 SQL。

## 用户与资料

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `POST /user/login` | `users`, `user_profiles` | 用用户名或手机号查询用户，校验密码后返回 token 和用户资料 |
| `POST /user/register` | `users`, `user_profiles` | 创建账号并初始化资料 |
| `GET /user/me` | `users`, `user_profiles` | 返回当前登录用户基础信息 |
| `GET /user/profile` | `users`, `user_profiles`, `favorites`, `travel_logs` | 返回个人资料、收藏数、日记数 |
| `PUT /user/profile` | `user_profiles` | 更新昵称、签名、性别、生日 |
| `POST /user/avatar` | `user_profiles` | 上传后更新头像 URL |
| `DELETE /user/avatar` | `user_profiles` | 清空或恢复默认头像 |
| `POST /user/logout` | 无强制写表 | JWT 模式下前端清 token 即可 |
| `POST /admin/images/poi/{id}` | `pois.image_url`, `spots.cover_image` | 管理员上传 POI/景点展示图，写入 OSS URL |
| `POST /admin/images/spot/{id}` | `spots.cover_image`, `pois.image_url` | 管理员按景点主表上传封面图，并同步代表 POI 图 |
| `POST /admin/images/food/{id}` | `foods.image_url` | 管理员上传美食展示图，写入 OSS URL |

## 设施 / 景点查询

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /search/facilities` | `pois`, `poi_categories`, `place_groups` | 按关键词、类型、区域、空间分组分页查询 POI |
| `GET /search/facility/{id}` | `pois`, `poi_categories`, `place_groups` | 查询单个 POI 详情 |
| `GET /search/types` | `poi_categories` | 返回设施/景点类型 |

字段转换建议：

| 前端字段 | 数据来源 |
| --- | --- |
| `type` | `poi_categories.code` |
| `typeName` / `categoryName` | `poi_categories.name` |
| `location` | `pois.location_text` |
| `image` | `pois.image_url` |
| `score` | `pois.rating` |
| `visits` | `pois.visit_count` |
| `placeGroupName` | `place_groups.name` |

## 个性化推荐

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /recommend/list` | `spots`, `poi_categories`, `spot_tag_relations`, `user_preferences`, `favorites`, `browsing_history` | 按推荐策略和游览场景返回景点主表数据，避免把景点内部 POI 当成推荐对象 |
| `GET /recommend/hot-top10` | `spots`, `poi_categories` | 按热度、评分、访问量返回热门景点，前四位固定优先展示北邮/北航四个校区 |
| `POST /recommend/rating` | `ratings` | 保存用户评分，可异步更新 POI 或美食均分 |

推荐分数建议：

```text
score =
  tag_match_score * 0.30
  + community_hot_score * 0.25
  + behavior_score * 0.25
  + rating_score * 0.10
  + distance_score * 0.10
```

## 美食

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /food/list` | `foods`, `pois` | 按菜系、价格、排序查询美食 |
| `GET /food/list/{id}` | `foods`, `pois` | 查询美食详情和所属地点 |
| `GET /food/recommend` | `foods`, `food_reviews`, `favorites`, `browsing_history` | 按评分、热度、行为推荐美食 |
| `POST /food/review` | `food_reviews` | 写入美食评价 |

字段转换建议：

| 前端字段 | 数据来源 |
| --- | --- |
| `score` | `foods.rating` |
| `rating` | `foods.rating` |
| `priceLevel` | `foods.price_level` |
| `avgPrice` | `foods.avg_price` |
| `cuisine` | `foods.cuisine_code` |
| `cuisineName` / `cuisine_type` | `foods.cuisine_name` |
| `spot_name` | `pois.name` |

## 路线规划

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /route/pois` | `pois`, `poi_categories` | 返回可选路线节点 |
| `POST /route/shortest` | `pois`, `route_edges`, `route_records`, `route_record_points` | 基于图边执行最短路径 |
| `POST /route/optimal` | `pois`, `route_edges`, `route_records`, `route_record_points` | 多点路线规划 |
| `POST /route/indoor` | `pois`, `route_edges` | 筛选 `is_indoor = 1` 的边 |

实现建议：

- `route_edges.distance_m` 可作为 Dijkstra 权重。
- 如果用户选择“时间最短”，可用 `duration_min * congestion_factor` 作为权重。
- `route_records` 和 `route_record_points` 可以保存一次规划结果，也可以第一版只返回临时计算结果。

## 游记 / 日志

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /log/list` | `travel_logs`, `users`, `user_profiles`, `pois`, `log_images`, `log_likes`, `log_comments`, `log_tag_relations`, `tags` | 按用户、POI、分页、tab 查询日志，返回可选多维评分 |
| `GET /log/my` | 同上 | 按当前登录 token 查询“我的日志”，避免前端传错 userId |
| `GET /log/{id}` | 同上 | 查询单条日志详情 |
| `POST /log/create` | `travel_logs`, `log_images`, `log_tag_relations`, `pois`, `spots` | 创建个人游记；评分可选，带评分时按维度生成综合分并刷新景点均分 |
| `DELETE /log/{id}` | `travel_logs` | 软删除日志；作者可删自己的，管理员可删任意日志 |
| `POST /log/{id}/like` | `log_likes` | 点赞或取消点赞 |

聚合字段建议：

| 前端字段 | 数据来源 |
| --- | --- |
| `username` | `user_profiles.nickname` 或 `users.username` |
| `like_count` | `COUNT(log_likes.user_id)` |
| `comment_count` | `COUNT(log_comments.id)` |
| `images` | `log_images.image_url` 聚合 |
| `tags` | `tags.name` 聚合 |
| `location` | `pois.name` |

注意：

- 当前前端部分点赞逻辑读取 `res.liked`，后端可以临时兼容顶层字段，后续再统一为 `res.data.liked`。

## 圈子社区

| 接口 | 主要表 | 说明 |
| --- | --- | --- |
| `GET /circle/list` | `circles`, `circle_members`, `travel_logs` | 返回已加入和未加入圈子 |
| `POST /circle/create` | `circles`, `circle_members` | 创建圈子并把创建者加入为圈主 |
| `GET /circle/{id}` | `circles`, `circle_members`, `users` | 查询圈子详情和成员列表 |
| `POST /circle/{id}/join` | `circle_members` | 加入圈子 |
| `POST /circle/{id}/leave` | `circle_members` | 退出圈子 |
| `GET /circle/{id}/logs` | `travel_logs`, `users`, `log_images`, `log_likes`, `log_comments` | 查询圈子日志 |
| `POST /circle/{id}/logs` | `travel_logs`, `log_images` | 发布圈子日志 |
| `POST /circle/{logId}/like` | `log_likes` | 点赞或取消点赞圈子日志 |
| `GET /circle/{logId}/comments` | `log_comments`, `users` | 查询评论和回复 |
| `POST /circle/{logId}/comments` | `log_comments` | 发布评论或回复 |

聚合字段建议：

| 前端字段 | 数据来源 |
| --- | --- |
| `members` | `COUNT(circle_members.user_id)` |
| `posts` / `logs` | `COUNT(travel_logs.id)` |
| `is_member` | 当前用户是否存在于 `circle_members` |
| `member_list` | `circle_members` join `users` |
| `cover` | `circles.cover_url` |

## 数据库对前端字段的总体原则

- 前端展示字段不一定都直接落库。
- 数量类字段优先通过聚合得到。
- 距离、推荐分数、推荐理由优先由后端计算。
- 图片先存 URL，不存二进制。
- 用户行为表要保留，因为它们是推荐创新点的依据。
- 高校和校区不使用专用表，而是作为 `place_groups` 的一种类型；普通景区、公园、商圈也使用同一张表。
