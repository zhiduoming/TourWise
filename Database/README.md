# TourWise 数据库 SQL 文件说明

这个目录里同时存在“当前建库脚本”“演示种子数据”“历史迁移脚本”“路线数据修正脚本”和“备份文件”。之前看起来很乱，核心原因是项目开发过程中多次重构了景点、POI、路线、日志、圈子和后台管理模块，但旧 SQL 没有删除。

现在建议按下面方式理解：

```text
schema.sql                         当前数据库结构基线
seed.sql                           基础演示数据
import_current_demo.sql            推荐的一键导入顺序入口
migration_*.sql                    旧库升级/专项数据修正脚本
*_seed.sql                         早期补充种子数据
backups/                           执行高风险路线重建前的备份
*.md                               数据库说明文档
```

## 1. 当前推荐入口

如果是新建一个干净数据库，优先看：

```text
Database/import_current_demo.sql
```

它按当前项目状态整理了推荐导入顺序。

执行方式：

```bash
mysql -uroot -p < Database/import_current_demo.sql
```

如果你已经在 IDEA 里连接了 MySQL，也可以直接打开这个文件，从上到下执行。

## 2. 当前核心文件

### `schema.sql`

当前数据库结构基线。

包含：

- 用户与资料
- 景点主表 `spots`
- 内部 POI 表 `pois`
- 标签
- 收藏、浏览、评分、用户行为
- 美食
- 路线边、路线节点、路线版本
- 圈子
- 行程规划
- 通知
- 旅行日志、评论、点赞、图片
- 内容举报
- 景点搜索视图

注意：

- `schema.sql` 会 `DROP TABLE`，执行前要确认当前库可以被清空。
- 现在已经补入 `ai_summary` 和 Huffman 压缩相关字段，新库不需要再额外执行对应加字段迁移。

### `seed.sql`

基础演示数据。

包含：

- 演示用户
- 基础空间分组
- POI 分类
- 标签
- 第一批 POI
- 收藏、浏览、评分
- 美食
- 圈子
- 日志、评论、点赞

注意：

- 这个文件仍保留早期沙河 POI 和路线数据。
- 当前精修数据会在后续重建脚本中覆盖北邮沙河和西土城旧 POI。

### `import_current_demo.sql`

当前推荐导入顺序。

它不会重新定义 SQL 内容，只负责按顺序 `SOURCE` 其他文件。

## 3. 当前仍建议保留的迁移脚本

这些脚本对当前项目仍有意义。

| 文件 | 用途 |
| --- | --- |
| `migration_add_campus_summary_pois.sql` | 给北邮、北航四个校区补代表 POI，用于首页 Top10 和景点查询 |
| `migration_add_poi_province_city.sql` | 给旧 POI 补省市字段并回填 |
| `migration_refactor_spot_poi_route_schema.sql` | 将旧 POI 数据迁移成“景点主表 + 内部 POI + 路线节点”的结构 |
| `migration_reassign_seed_logs_from_admin.sql` | 把演示日志从管理员账号迁移到普通用户，避免个人主页误显示 |
| `migration_rebuild_shahe_exact_pois_routes.sql` | 重建北邮沙河校区精修 POI 和路线边 |
| `migration_rebuild_xitucheng_exact_pois.sql` | 重建北邮西土城校区精修 POI，路线边留给后台标注 |
| `migration_add_log_huffman_compression.sql` | 给旧库补 Huffman 压缩字段；新库已包含在 `schema.sql` 中 |
| `migration_add_ai_summary.sql` | 给旧库补 AI 景点简介字段；新库已包含在 `schema.sql` 中 |

## 4. 历史迁移脚本

这些脚本主要用于记录开发过程，或者用于“旧数据库逐步升级”。如果你是从干净库开始，通常不需要单独执行。

| 文件 | 说明 |
| --- | --- |
| `migration_add_admin_role_and_route_maps.sql` | 管理员角色和路线底图表，当前已合入 `schema.sql` |
| `migration_add_content_reports.sql` | 内容举报表，当前已合入 `schema.sql` |
| `migration_add_files_table.sql` | 文件表，当前已合入 `schema.sql` |
| `migration_add_itinerary_plans.sql` | 行程规划表，当前已合入 `schema.sql` |
| `migration_add_itinerary_favorites_hot.sql` | 行程收藏、复制和热度字段，当前已合入 `schema.sql` |
| `migration_add_log_dimension_ratings.sql` | 日志多维评分，当前已合入 `schema.sql` |
| `migration_add_log_itinerary_plan.sql` | 日志关联行程，当前已合入 `schema.sql` |
| `migration_add_notifications.sql` | 通知表，当前已合入 `schema.sql` |
| `migration_add_preference_action_sources.sql` | 推荐偏好来源枚举扩展，当前结构已包含 |
| `migration_add_recommend_dislike_feedback.sql` | 推荐负反馈枚举扩展，当前结构已包含 |
| `migration_add_route_graph_versions.sql` | 路网版本表，当前已合入 `schema.sql` |
| `migration_add_shahe_map_coordinates.sql` | 沙河第一版地图坐标，已被后续精修脚本替代 |
| `migration_add_user_spot_actions.sql` | 用户想去/去过/不感兴趣行为表，当前已合入 `schema.sql` |
| `migration_route_modes_location_and_bupt_rebuild.sql` | 路线模式拆分早期重建脚本，已被后续沙河/西土城精修脚本替代 |
| `migration_fix_shahe_route_graph_map_aligned.sql` | 沙河路网中间修正版，已被最终精修脚本替代 |
| `migration_fix_shahe_precise_coords_graph.sql` | 沙河坐标中间修正版，已被最终精修脚本替代 |
| `migration_refine_shahe_south_west_route.sql` | 沙河南门到西门早期修正，已被最终精修脚本替代 |

## 5. 早期种子文件

| 文件 | 当前建议 |
| --- | --- |
| `campus_poi_seed.sql` | 早期四校区内部 POI 演示数据，当前精修脚本会覆盖北邮两校区 |
| `bupt_route_edges_seed.sql` | 早期北邮路线边，当前沙河会被精修脚本覆盖，西土城路线建议后台重新标注 |
| `placeholder_poi_seed.sql` | 全国热门高校和景区占位数据，仍可用于列表、搜索、分页和推荐演示 |
| `route_edges_bupt_shahe_realistic.sql` | 早期沙河路网数据，当前不建议再导入 |

## 6. 备份文件

```text
Database/backups/
```

这些是执行路线/POI 重建前导出的备份。不要随便删，尤其是你还在继续调路线数据的时候。

## 7. 推荐导入顺序

当前干净库推荐执行：

```text
1. schema.sql
2. seed.sql
3. campus_poi_seed.sql
4. placeholder_poi_seed.sql
5. migration_add_campus_summary_pois.sql
6. migration_add_poi_province_city.sql
7. migration_refactor_spot_poi_route_schema.sql
8. migration_reassign_seed_logs_from_admin.sql
9. migration_rebuild_shahe_exact_pois_routes.sql
10. migration_rebuild_xitucheng_exact_pois.sql
```

如果你只想最小化启动后端，可以先执行：

```text
1. schema.sql
2. seed.sql
```

但这样路线规划、景点查询和当前精修数据不会完整。

## 8. 后续整理建议

现在我没有删除任何 SQL 文件，只做了说明和入口整理。

后续如果你确认要进一步清理，可以考虑：

1. 新建 `Database/archive/`。
2. 把历史中间态迁移脚本移动进去。
3. 保留根目录下的核心文件：
   - `schema.sql`
   - `seed.sql`
   - `import_current_demo.sql`
   - `migration_rebuild_shahe_exact_pois_routes.sql`
   - `migration_rebuild_xitucheng_exact_pois.sql`
   - `placeholder_poi_seed.sql`
4. 再补一份最终版 `schema_current.sql + seed_current.sql`。

这一步涉及移动文件，虽然不是业务删除，但会改变目录结构，建议你确认后再做。
