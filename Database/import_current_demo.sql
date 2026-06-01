-- TourWise 当前推荐导入入口
-- 用途：从一个干净 MySQL 实例重建当前演示库。
--
-- 执行方式：
--   mysql -uroot -p < Database/import_current_demo.sql
--
-- 注意：
-- 1. schema.sql 会 DROP 并重建相关表，执行前确认 tourist_system 可以被清空。
-- 2. 本文件只整理当前推荐顺序，不删除任何历史 SQL。
-- 3. 如果你已经在某个旧库上反复导入过，建议先备份，再重新执行本入口。

SOURCE Database/schema.sql;
SOURCE Database/seed.sql;

-- 早期演示数据：用于补充四校区、全国热门景点和高校。
SOURCE Database/campus_poi_seed.sql;
SOURCE Database/placeholder_poi_seed.sql;

-- 让四个校区在首页 Top10 和景点查询中作为“大景点”出现。
SOURCE Database/migration_add_campus_summary_pois.sql;

-- 给旧 POI 回填省市，支持省份、城市、简称、标签组合查询。
SOURCE Database/migration_add_poi_province_city.sql;

-- 景点主表 / 内部 POI / 路线节点结构重构。
SOURCE Database/migration_refactor_spot_poi_route_schema.sql;

-- 演示日志从管理员账号迁出，避免管理员个人主页默认显示 seed 日志。
SOURCE Database/migration_reassign_seed_logs_from_admin.sql;

-- 当前精修样板：北邮沙河校区。
-- 会重建 place_group_id = 4 的 POI 和路线边。
SOURCE Database/migration_rebuild_shahe_exact_pois_routes.sql;

-- 当前精修样板：北邮西土城校区。
-- 会重建 place_group_id = 3 的 POI；路线边留给后台路线标注工具重新绘制。
SOURCE Database/migration_rebuild_xitucheng_exact_pois.sql;

-- 旧库兼容迁移。
-- 新库的 schema.sql 已包含这些字段；这里保留执行是为了避免旧 schema 漏字段。
SOURCE Database/migration_add_ai_summary.sql;
SOURCE Database/migration_add_log_huffman_compression.sql;
