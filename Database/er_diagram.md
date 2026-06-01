# E-R 图说明

下面的 Mermaid 图展示数据库核心实体关系。可复制到支持 Mermaid 的 Markdown 编辑器中预览。

```mermaid
erDiagram
    users ||--|| user_profiles : has
    users ||--o{ user_preferences : owns
    users ||--o{ favorites : creates
    users ||--o{ browsing_history : creates
    users ||--o{ ratings : creates
    users ||--o{ food_reviews : writes
    users ||--o{ travel_logs : writes
    users ||--o{ log_likes : likes
    users ||--o{ log_comments : comments
    users ||--o{ circle_members : joins
    users ||--o{ circles : owns
    users ||--o{ route_records : creates

    place_groups ||--o{ place_groups : parent
    place_groups ||--o{ pois : contains
    poi_categories ||--o{ pois : classifies
    poi_categories ||--o{ poi_categories : parent
    pois ||--o{ poi_tag_relations : has
    tags ||--o{ poi_tag_relations : marks
    tags ||--o{ user_preferences : describes
    tags ||--o{ log_tag_relations : marks

    pois ||--o{ foods : contains
    pois ||--o{ route_edges : from_node
    pois ||--o{ route_edges : to_node
    pois ||--o{ route_record_points : route_point
    pois ||--o{ travel_logs : referenced_by

    foods ||--o{ food_reviews : receives

    route_records ||--o{ route_record_points : contains

    circles ||--o{ circle_members : has
    circles ||--o{ travel_logs : contains

    travel_logs ||--o{ log_images : has
    travel_logs ||--o{ log_tag_relations : has
    travel_logs ||--o{ log_likes : receives
    travel_logs ||--o{ log_comments : has
    log_comments ||--o{ log_comments : replies
```

## 关系解释

- `users` 与 `user_profiles` 是一对一关系，账号信息和展示资料分离。
- `place_groups` 是通用空间分组，可表示高校、校区、景区、公园、商圈等。
- `pois` 是地点核心表，通过 `place_groups` 表示空间归属，通过 `poi_categories` 表示类型，通过 `tags` 表示兴趣特征。
- `pois` 与 `route_edges` 构成路线图，POI 是节点，路线边是边。
- `travel_logs` 同时支持个人游记和圈子日志：
  - `circle_id` 为空时表示个人游记。
  - `circle_id` 不为空时表示圈子日志。
- `log_likes`、`log_comments`、`favorites`、`browsing_history`、`ratings` 共同构成用户行为数据。
- `user_preferences` 可以来自用户手动选择，也可以由浏览、收藏、点赞、评论、评分等行为沉淀。
