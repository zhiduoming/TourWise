-- 个性化旅游系统数据库结构
-- MySQL 8.x / utf8mb4

CREATE DATABASE IF NOT EXISTS tourist_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE tourist_system;

SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS v_spot_search;

DROP TABLE IF EXISTS content_reports;
DROP TABLE IF EXISTS log_comments;
DROP TABLE IF EXISTS log_likes;
DROP TABLE IF EXISTS log_tag_relations;
DROP TABLE IF EXISTS log_images;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS travel_logs;
DROP TABLE IF EXISTS itinerary_plan_favorites;
DROP TABLE IF EXISTS itinerary_plan_items;
DROP TABLE IF EXISTS itinerary_plans;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS circle_members;
DROP TABLE IF EXISTS circles;
DROP TABLE IF EXISTS route_node_edges;
DROP TABLE IF EXISTS route_nodes;
DROP TABLE IF EXISTS route_record_points;
DROP TABLE IF EXISTS route_records;
DROP TABLE IF EXISTS route_edges;
DROP TABLE IF EXISTS route_graph_versions;
DROP TABLE IF EXISTS place_group_maps;
DROP TABLE IF EXISTS food_reviews;
DROP TABLE IF EXISTS foods;
DROP TABLE IF EXISTS ratings;
DROP TABLE IF EXISTS user_spot_actions;
DROP TABLE IF EXISTS browsing_history;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS user_preferences;
DROP TABLE IF EXISTS poi_tag_relations;
DROP TABLE IF EXISTS spot_tag_relations;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS pois;
DROP TABLE IF EXISTS spots;
DROP TABLE IF EXISTS poi_categories;
DROP TABLE IF EXISTS place_groups;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
  role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: user普通用户 admin管理员',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE user_profiles (
  user_id BIGINT PRIMARY KEY COMMENT '用户ID',
  nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  signature VARCHAR(120) DEFAULT NULL COMMENT '个性签名',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  gender ENUM('male', 'female', 'secret') NOT NULL DEFAULT 'secret' COMMENT '性别',
  birthday DATE DEFAULT NULL COMMENT '生日',
  visit_count INT NOT NULL DEFAULT 0 COMMENT '主页访问数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';

CREATE TABLE place_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '空间分组ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级空间分组ID',
  name VARCHAR(100) NOT NULL COMMENT '分组名称, 如北京邮电大学/沙河校区/奥林匹克森林公园',
  short_name VARCHAR(50) DEFAULT NULL COMMENT '简称',
  group_type ENUM('university', 'campus', 'scenic_area', 'business_area', 'park', 'museum_area', 'city_area', 'other') NOT NULL COMMENT '分组类型',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  district VARCHAR(50) DEFAULT NULL COMMENT '行政区',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心纬度',
  description VARCHAR(500) DEFAULT NULL COMMENT '分组说明',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  route_graph_status VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '内部路网状态: none/draft/verified',
  KEY idx_place_groups_parent (parent_id),
  KEY idx_place_groups_type_city (group_type, city),
  CONSTRAINT fk_place_groups_parent FOREIGN KEY (parent_id) REFERENCES place_groups(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间分组表, 可表示高校/校区/景区/公园/商圈等';

CREATE TABLE place_group_maps (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地图图片ID',
  spot_id BIGINT DEFAULT NULL COMMENT '所属景点ID，重构后指向 spots.id',
  place_group_id BIGINT NOT NULL COMMENT '空间分组ID',
  image_url VARCHAR(255) NOT NULL COMMENT '地图图片URL',
  original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  map_width INT DEFAULT NULL COMMENT '地图图片像素宽度',
  map_height INT DEFAULT NULL COMMENT '地图图片像素高度',
  uploaded_by BIGINT DEFAULT NULL COMMENT '上传管理员ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_place_group_maps_group (place_group_id),
  KEY idx_place_group_maps_spot (spot_id),
  KEY idx_place_group_maps_uploaded_by (uploaded_by),
  CONSTRAINT fk_place_group_maps_group FOREIGN KEY (place_group_id) REFERENCES place_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_place_group_maps_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点/校区路线底图表';

CREATE TABLE route_graph_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路网版本ID',
  place_group_id BIGINT NOT NULL COMMENT '空间分组ID',
  version_no INT NOT NULL COMMENT '版本号',
  name VARCHAR(100) NOT NULL COMMENT '版本名称',
  snapshot_json LONGTEXT NOT NULL COMMENT '路网快照JSON',
  node_count INT NOT NULL DEFAULT 0 COMMENT '节点数量',
  edge_count INT NOT NULL DEFAULT 0 COMMENT '无向路段数量',
  created_by BIGINT DEFAULT NULL COMMENT '创建管理员ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_route_graph_versions_no (place_group_id, version_no),
  KEY idx_route_graph_versions_group_time (place_group_id, created_at),
  KEY idx_route_graph_versions_created_by (created_by),
  CONSTRAINT fk_route_graph_versions_group FOREIGN KEY (place_group_id) REFERENCES place_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_graph_versions_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点内部路网版本快照表';

CREATE TABLE poi_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
  code VARCHAR(50) NOT NULL COMMENT '分类编码, 如 library/scenic',
  name VARCHAR(50) NOT NULL COMMENT '分类名称',
  parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
  scene ENUM('campus', 'city', 'both') NOT NULL DEFAULT 'both' COMMENT '适用场景',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  UNIQUE KEY uk_poi_categories_code (code),
  KEY idx_poi_categories_parent (parent_id),
  CONSTRAINT fk_poi_categories_parent FOREIGN KEY (parent_id) REFERENCES poi_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='POI分类表';

CREATE TABLE spots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '景点ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级景点ID，如高校本部 -> 校区',
  legacy_place_group_id BIGINT DEFAULT NULL COMMENT '来源 place_groups.id，兼容旧空间分组',
  legacy_poi_id BIGINT DEFAULT NULL COMMENT '来源 pois.id，兼容旧景点型POI',
  representative_poi_id BIGINT DEFAULT NULL COMMENT '兼容旧前端详情页的代表POI ID',
  category_id BIGINT DEFAULT NULL COMMENT '景点分类ID',
  name VARCHAR(100) NOT NULL COMMENT '景点名称',
  short_name VARCHAR(50) DEFAULT NULL COMMENT '简称',
  spot_type VARCHAR(50) NOT NULL DEFAULT 'other' COMMENT '景点类型: university/campus/scenic/museum/park/landmark/business/other',
  province VARCHAR(50) DEFAULT NULL COMMENT '省份/直辖市/自治区',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  district VARCHAR(50) DEFAULT NULL COMMENT '区县',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  description TEXT COMMENT '景点介绍',
  cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '中心纬度',
  location_radius_m INT NOT NULL DEFAULT 500 COMMENT '定位匹配半径，单位米',
  rating DECIMAL(3, 1) NOT NULL DEFAULT 5.0 COMMENT '评分',
  hotness INT NOT NULL DEFAULT 0 COMMENT '热度',
  visit_count INT NOT NULL DEFAULT 0 COMMENT '访问量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_spots_legacy_place_group (legacy_place_group_id),
  UNIQUE KEY uk_spots_legacy_poi (legacy_poi_id),
  KEY idx_spots_parent (parent_id),
  KEY idx_spots_category (category_id),
  KEY idx_spots_region (province, city),
  KEY idx_spots_type_hotness (spot_type, hotness),
  FULLTEXT KEY ft_spots_name_desc (name, description),
  CONSTRAINT fk_spots_parent FOREIGN KEY (parent_id) REFERENCES spots(id) ON DELETE SET NULL,
  CONSTRAINT fk_spots_place_group FOREIGN KEY (legacy_place_group_id) REFERENCES place_groups(id) ON DELETE SET NULL,
  CONSTRAINT fk_spots_category FOREIGN KEY (category_id) REFERENCES poi_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点主表，存景区/高校/校区/商圈等大地点';

CREATE TABLE pois (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'POI ID',
  spot_id BIGINT DEFAULT NULL COMMENT '所属景点ID，重构后指向 spots.id',
  poi_role VARCHAR(30) NOT NULL DEFAULT 'internal' COMMENT 'POI角色: internal内部点位 spot_legacy旧景点行 route_legacy旧路线节点',
  place_group_id BIGINT DEFAULT NULL COMMENT '所属空间分组ID',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  name VARCHAR(100) NOT NULL COMMENT '名称',
  scene ENUM('campus', 'city') NOT NULL DEFAULT 'campus' COMMENT '场景: 校园或城市',
  area_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码, 如 teaching/living/sports',
  area_name VARCHAR(50) DEFAULT NULL COMMENT '区域名称',
  province VARCHAR(50) DEFAULT NULL COMMENT '省份/直辖市/自治区',
  city VARCHAR(50) DEFAULT NULL COMMENT '城市',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  location_text VARCHAR(100) DEFAULT NULL COMMENT '前端展示位置',
  description TEXT COMMENT '介绍',
  image_url VARCHAR(255) DEFAULT NULL COMMENT '图片地址',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  map_x INT DEFAULT NULL COMMENT '平面图X坐标',
  map_y INT DEFAULT NULL COMMENT '平面图Y坐标',
  open_time TIME DEFAULT NULL COMMENT '开放时间',
  close_time TIME DEFAULT NULL COMMENT '关闭时间',
  phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  rating DECIMAL(3, 1) NOT NULL DEFAULT 5.0 COMMENT '综合评分',
  hotness INT NOT NULL DEFAULT 0 COMMENT '热度',
  visit_count INT NOT NULL DEFAULT 0 COMMENT '访问量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_pois_place_group (place_group_id),
  KEY idx_pois_category (category_id),
  KEY idx_pois_scene_area (scene, area_code),
  KEY idx_pois_hotness (hotness),
  KEY idx_pois_rating (rating),
  FULLTEXT KEY ft_pois_name_desc (name, description),
  CONSTRAINT fk_pois_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE SET NULL,
  CONSTRAINT fk_pois_place_group FOREIGN KEY (place_group_id) REFERENCES place_groups(id) ON DELETE SET NULL,
  CONSTRAINT fk_pois_category FOREIGN KEY (category_id) REFERENCES poi_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='POI表, 统一表示校园设施和城市景点';

CREATE TABLE tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
  name VARCHAR(50) NOT NULL COMMENT '标签名称',
  tag_type ENUM('interest', 'poi', 'food', 'log', 'circle') NOT NULL DEFAULT 'interest' COMMENT '标签类型',
  UNIQUE KEY uk_tags_name_type (name, tag_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE poi_tag_relations (
  poi_id BIGINT NOT NULL COMMENT 'POI ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (poi_id, tag_id),
  KEY idx_poi_tag_relations_tag (tag_id),
  CONSTRAINT fk_poi_tags_poi FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE CASCADE,
  CONSTRAINT fk_poi_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='POI标签关联表';

CREATE TABLE spot_tag_relations (
  spot_id BIGINT NOT NULL COMMENT '景点ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (spot_id, tag_id),
  KEY idx_spot_tags_tag (tag_id),
  CONSTRAINT fk_spot_tags_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE,
  CONSTRAINT fk_spot_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点标签关联表';

CREATE TABLE user_preferences (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '偏好ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  weight DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '偏好权重',
  source ENUM('manual', 'browse', 'favorite', 'want', 'visited', 'dislike', 'like', 'comment', 'rating', 'circle') NOT NULL DEFAULT 'manual' COMMENT '偏好来源',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_preferences_user_tag_source (user_id, tag_id, source),
  KEY idx_user_preferences_tag (tag_id),
  CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_preferences_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣偏好表';

CREATE TABLE favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type ENUM('poi', 'food', 'log') NOT NULL COMMENT '收藏对象类型',
  target_id BIGINT NOT NULL COMMENT '收藏对象ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_favorites_user_target (user_id, target_type, target_id),
  KEY idx_favorites_target (target_type, target_id),
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE browsing_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '浏览记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type ENUM('poi', 'food', 'log', 'circle') NOT NULL COMMENT '浏览对象类型',
  target_id BIGINT NOT NULL COMMENT '浏览对象ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  KEY idx_browsing_user_time (user_id, created_at),
  KEY idx_browsing_target (target_type, target_id),
  CONSTRAINT fk_browsing_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录表';

CREATE TABLE user_spot_actions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户景点行为状态ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type ENUM('poi') NOT NULL DEFAULT 'poi' COMMENT '行为对象类型',
  target_id BIGINT NOT NULL COMMENT '行为对象ID',
  action_type ENUM('want', 'visited', 'dislike') NOT NULL COMMENT '行为类型: 想去/去过/不感兴趣',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效: 1有效 0取消',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_spot_actions_user_target_action (user_id, target_type, target_id, action_type),
  KEY idx_user_spot_actions_target (target_type, target_id),
  KEY idx_user_spot_actions_user_action (user_id, action_type, active, updated_at),
  CONSTRAINT fk_user_spot_actions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户景点想去/去过状态表';

CREATE TABLE ratings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type ENUM('poi', 'food', 'log') NOT NULL COMMENT '评分对象类型',
  target_id BIGINT NOT NULL COMMENT '评分对象ID',
  rating DECIMAL(3, 1) NOT NULL COMMENT '评分',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_ratings_user_target (user_id, target_type, target_id),
  KEY idx_ratings_target (target_type, target_id),
  CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT ck_ratings_rating CHECK (rating >= 0 AND rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分表';

CREATE TABLE foods (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '美食ID',
  poi_id BIGINT DEFAULT NULL COMMENT '所属POI, 如食堂/商圈/景区',
  name VARCHAR(100) NOT NULL COMMENT '美食名称',
  cuisine_code VARCHAR(50) DEFAULT NULL COMMENT '菜系编码',
  cuisine_name VARCHAR(50) DEFAULT NULL COMMENT '菜系名称',
  description TEXT COMMENT '介绍',
  image_url VARCHAR(255) DEFAULT NULL COMMENT '图片地址',
  price_level TINYINT NOT NULL DEFAULT 2 COMMENT '价格等级: 1实惠 2适中 3较高',
  avg_price DECIMAL(8, 2) DEFAULT NULL COMMENT '人均价格',
  rating DECIMAL(3, 1) NOT NULL DEFAULT 5.0 COMMENT '评分',
  hotness INT NOT NULL DEFAULT 0 COMMENT '热度',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  open_time TIME DEFAULT NULL COMMENT '营业开始时间',
  close_time TIME DEFAULT NULL COMMENT '营业结束时间',
  phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_foods_poi (poi_id),
  KEY idx_foods_cuisine_price (cuisine_code, price_level),
  KEY idx_foods_rating (rating),
  CONSTRAINT fk_foods_poi FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美食表';

CREATE TABLE food_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '美食评价ID',
  food_id BIGINT NOT NULL COMMENT '美食ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  rating DECIMAL(3, 1) DEFAULT NULL COMMENT '评分',
  content VARCHAR(1000) NOT NULL COMMENT '评价内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_food_reviews_food (food_id),
  KEY idx_food_reviews_user (user_id),
  CONSTRAINT fk_food_reviews_food FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE,
  CONSTRAINT fk_food_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美食评价表';

CREATE TABLE files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
  original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
  url VARCHAR(255) NOT NULL COMMENT '访问URL',
  mime_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  size BIGINT DEFAULT NULL COMMENT '文件大小, 字节',
  upload_by BIGINT DEFAULT NULL COMMENT '上传用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_files_upload_by (upload_by),
  CONSTRAINT fk_files_upload_by FOREIGN KEY (upload_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件表';

CREATE TABLE route_nodes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线节点ID',
  spot_id BIGINT NOT NULL COMMENT '所属景点ID',
  legacy_poi_id BIGINT DEFAULT NULL COMMENT '来源pois.id，兼容旧路线图',
  name VARCHAR(100) NOT NULL COMMENT '节点名称',
  node_type VARCHAR(30) NOT NULL DEFAULT 'intersection' COMMENT '节点类型: poi/gate/intersection/entrance',
  visible TINYINT NOT NULL DEFAULT 0 COMMENT '是否展示给用户选择',
  map_x INT DEFAULT NULL COMMENT '平面图X坐标',
  map_y INT DEFAULT NULL COMMENT '平面图Y坐标',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_route_nodes_legacy_poi (legacy_poi_id),
  KEY idx_route_nodes_spot_visible (spot_id, visible),
  CONSTRAINT fk_route_nodes_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线算法节点表，路口/入口/可通行点单独存放';

CREATE TABLE route_node_edges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线边ID',
  spot_id BIGINT NOT NULL COMMENT '所属景点ID',
  from_node_id BIGINT NOT NULL COMMENT '起点路线节点ID',
  to_node_id BIGINT NOT NULL COMMENT '终点路线节点ID',
  distance_m INT NOT NULL COMMENT '距离，单位米',
  duration_min INT NOT NULL COMMENT '预计耗时，单位分钟',
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk' COMMENT '通行方式',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '拥挤系数',
  is_indoor TINYINT NOT NULL DEFAULT 0 COMMENT '是否室内路线',
  description VARCHAR(255) DEFAULT NULL COMMENT '路段说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_route_node_edges_unique (from_node_id, to_node_id, transport_type),
  KEY idx_route_node_edges_spot (spot_id),
  KEY idx_route_node_edges_from (from_node_id),
  KEY idx_route_node_edges_to (to_node_id),
  CONSTRAINT fk_route_node_edges_spot FOREIGN KEY (spot_id) REFERENCES spots(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_node_edges_from FOREIGN KEY (from_node_id) REFERENCES route_nodes(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_node_edges_to FOREIGN KEY (to_node_id) REFERENCES route_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线算法边表，连接route_nodes';

CREATE TABLE route_edges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线边ID',
  from_poi_id BIGINT NOT NULL COMMENT '起点POI',
  to_poi_id BIGINT NOT NULL COMMENT '终点POI',
  distance_m INT NOT NULL COMMENT '距离, 单位米',
  duration_min INT NOT NULL COMMENT '预计耗时, 单位分钟',
  transport_type ENUM('walk', 'bike', 'bus', 'indoor') NOT NULL DEFAULT 'walk' COMMENT '通行方式',
  congestion_factor DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT '拥挤系数',
  is_indoor TINYINT NOT NULL DEFAULT 0 COMMENT '是否室内路线',
  description VARCHAR(255) DEFAULT NULL COMMENT '路段说明',
  KEY idx_route_edges_from (from_poi_id),
  KEY idx_route_edges_to (to_poi_id),
  CONSTRAINT fk_route_edges_from FOREIGN KEY (from_poi_id) REFERENCES pois(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_edges_to FOREIGN KEY (to_poi_id) REFERENCES pois(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线边表, 支撑最短路径算法';

CREATE TABLE route_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线记录ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  route_name VARCHAR(100) DEFAULT NULL COMMENT '路线名称',
  mode ENUM('shortest', 'optimal', 'indoor') NOT NULL DEFAULT 'shortest' COMMENT '规划模式',
  total_distance_m INT DEFAULT NULL COMMENT '总距离',
  total_duration_min INT DEFAULT NULL COMMENT '总耗时',
  preferences JSON DEFAULT NULL COMMENT '路线偏好',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_route_records_user (user_id),
  CONSTRAINT fk_route_records_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线规划记录表';

CREATE TABLE route_record_points (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路线点ID',
  route_record_id BIGINT NOT NULL COMMENT '路线记录ID',
  poi_id BIGINT DEFAULT NULL COMMENT 'POI ID',
  point_name VARCHAR(100) NOT NULL COMMENT '点位名称',
  sort_order INT NOT NULL COMMENT '顺序',
  distance_from_start_m INT DEFAULT NULL COMMENT '距起点距离',
  description VARCHAR(255) DEFAULT NULL COMMENT '点位说明',
  KEY idx_route_points_record (route_record_id),
  KEY idx_route_points_poi (poi_id),
  CONSTRAINT fk_route_points_record FOREIGN KEY (route_record_id) REFERENCES route_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_route_points_poi FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线点表';

CREATE TABLE itinerary_plans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程计划ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  source_plan_id BIGINT DEFAULT NULL COMMENT '复制来源行程ID',
  title VARCHAR(100) NOT NULL COMMENT '行程标题',
  city VARCHAR(50) NOT NULL COMMENT '目的城市',
  duration VARCHAR(30) NOT NULL COMMENT '游玩时长',
  pace VARCHAR(30) NOT NULL COMMENT '游玩节奏',
  total_days INT NOT NULL DEFAULT 1 COMMENT '总天数',
  spot_count INT NOT NULL DEFAULT 0 COMMENT '景点数量',
  copy_count INT NOT NULL DEFAULT 0 COMMENT '被复制次数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '被收藏次数',
  summary VARCHAR(500) DEFAULT NULL COMMENT '行程摘要',
  preferences_json JSON DEFAULT NULL COMMENT '偏好JSON',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_itinerary_plans_user_time (user_id, status, updated_at),
  KEY idx_itinerary_plans_source (source_plan_id),
  KEY idx_itinerary_plans_hot (status, favorite_count, copy_count, updated_at),
  CONSTRAINT fk_itinerary_plans_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_itinerary_plans_source FOREIGN KEY (source_plan_id) REFERENCES itinerary_plans(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户旅行计划表';

CREATE TABLE itinerary_plan_favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程收藏ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  plan_id BIGINT NOT NULL COMMENT '行程计划ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_itinerary_favorite_user_plan (user_id, plan_id),
  KEY idx_itinerary_favorites_plan (plan_id),
  CONSTRAINT fk_itinerary_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_itinerary_favorites_plan FOREIGN KEY (plan_id) REFERENCES itinerary_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏行程表';

CREATE TABLE itinerary_plan_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行程节点ID',
  plan_id BIGINT NOT NULL COMMENT '行程计划ID',
  day_no INT NOT NULL COMMENT '第几天',
  order_no INT NOT NULL COMMENT '当天排序',
  item_type ENUM('spot', 'food') NOT NULL DEFAULT 'spot' COMMENT '节点类型',
  time_slot VARCHAR(30) DEFAULT NULL COMMENT '时间段',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID，景点为POI ID，美食为foods.id',
  spot_id BIGINT DEFAULT NULL COMMENT '关联景点ID',
  place_group_id BIGINT DEFAULT NULL COMMENT '关联空间分组ID',
  name VARCHAR(100) NOT NULL COMMENT '节点名称',
  description TEXT COMMENT '节点说明',
  address VARCHAR(255) DEFAULT NULL COMMENT '地址',
  image_url VARCHAR(255) DEFAULT NULL COMMENT '图片',
  rating DECIMAL(3, 1) DEFAULT NULL COMMENT '评分',
  hotness INT DEFAULT NULL COMMENT '热度',
  longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
  recommend_reason VARCHAR(255) DEFAULT NULL COMMENT '推荐理由',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_itinerary_plan_items_plan (plan_id, day_no, order_no),
  CONSTRAINT fk_itinerary_items_plan FOREIGN KEY (plan_id) REFERENCES itinerary_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户旅行计划节点表';

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  actor_user_id BIGINT DEFAULT NULL COMMENT '触发用户ID',
  type VARCHAR(30) NOT NULL COMMENT '通知类型: log_like/log_comment/itinerary_copy',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
  target_type VARCHAR(30) DEFAULT NULL COMMENT '目标类型',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID',
  link_url VARCHAR(255) DEFAULT NULL COMMENT '前端跳转地址',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_notifications_user_read_time (user_id, is_read, created_at),
  KEY idx_notifications_target (target_type, target_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

CREATE TABLE circles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '圈子ID',
  name VARCHAR(50) NOT NULL COMMENT '圈子名称',
  description VARCHAR(255) NOT NULL COMMENT '圈子描述',
  cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  owner_id BIGINT NOT NULL COMMENT '圈主ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_circles_owner (owner_id),
  FULLTEXT KEY ft_circles_name_desc (name, description),
  CONSTRAINT fk_circles_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兴趣圈子表';

CREATE TABLE circle_members (
  circle_id BIGINT NOT NULL COMMENT '圈子ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 1成员 2管理员 3圈主',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (circle_id, user_id),
  KEY idx_circle_members_user (user_id),
  CONSTRAINT fk_circle_members_circle FOREIGN KEY (circle_id) REFERENCES circles(id) ON DELETE CASCADE,
  CONSTRAINT fk_circle_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子成员表';

CREATE TABLE travel_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游记/日志ID',
  user_id BIGINT NOT NULL COMMENT '作者ID',
  poi_id BIGINT DEFAULT NULL COMMENT '关联POI',
  circle_id BIGINT DEFAULT NULL COMMENT '关联圈子',
  itinerary_plan_id BIGINT DEFAULT NULL COMMENT '关联保存行程',
  title VARCHAR(100) DEFAULT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '内容',
  rating DECIMAL(3, 1) DEFAULT NULL COMMENT '作者评分',
  scenery_rating DECIMAL(3, 1) DEFAULT NULL COMMENT '景观体验评分',
  facility_rating DECIMAL(3, 1) DEFAULT NULL COMMENT '设施完善评分',
  service_rating DECIMAL(3, 1) DEFAULT NULL COMMENT '服务体验评分',
  traffic_rating DECIMAL(3, 1) DEFAULT NULL COMMENT '交通便利评分',
  value_rating DECIMAL(3, 1) DEFAULT NULL COMMENT '性价比评分',
  hotness INT NOT NULL DEFAULT 0 COMMENT '热度',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_travel_logs_user (user_id),
  KEY idx_travel_logs_poi (poi_id),
  KEY idx_travel_logs_circle (circle_id),
  KEY idx_travel_logs_itinerary_plan (itinerary_plan_id),
  KEY idx_travel_logs_created (created_at),
  FULLTEXT KEY ft_travel_logs_title_content (title, content),
  CONSTRAINT fk_travel_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_travel_logs_poi FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE SET NULL,
  CONSTRAINT fk_travel_logs_circle FOREIGN KEY (circle_id) REFERENCES circles(id) ON DELETE SET NULL,
  CONSTRAINT fk_travel_logs_itinerary_plan FOREIGN KEY (itinerary_plan_id) REFERENCES itinerary_plans(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游记和圈子日志表';

CREATE TABLE log_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志图片ID',
  log_id BIGINT NOT NULL COMMENT '日志ID',
  image_url VARCHAR(255) NOT NULL COMMENT '图片地址',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  CONSTRAINT fk_log_images_log FOREIGN KEY (log_id) REFERENCES travel_logs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志图片表';

CREATE TABLE log_tag_relations (
  log_id BIGINT NOT NULL COMMENT '日志ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (log_id, tag_id),
  KEY idx_log_tags_tag (tag_id),
  CONSTRAINT fk_log_tags_log FOREIGN KEY (log_id) REFERENCES travel_logs(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志标签关联表';

CREATE TABLE log_likes (
  log_id BIGINT NOT NULL COMMENT '日志ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (log_id, user_id),
  KEY idx_log_likes_user (user_id),
  CONSTRAINT fk_log_likes_log FOREIGN KEY (log_id) REFERENCES travel_logs(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志点赞表';

CREATE TABLE log_comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  log_id BIGINT NOT NULL COMMENT '日志ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父评论ID',
  content VARCHAR(1000) NOT NULL COMMENT '评论内容',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0已删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_log_comments_log (log_id),
  KEY idx_log_comments_user (user_id),
  KEY idx_log_comments_parent (parent_id),
  CONSTRAINT fk_log_comments_log FOREIGN KEY (log_id) REFERENCES travel_logs(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_log_comments_parent FOREIGN KEY (parent_id) REFERENCES log_comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志评论表';

CREATE TABLE content_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
  target_type VARCHAR(20) NOT NULL COMMENT '举报对象类型: log/comment',
  target_id BIGINT NOT NULL COMMENT '举报对象ID',
  reporter_id BIGINT NOT NULL COMMENT '举报用户ID',
  reason VARCHAR(100) NOT NULL COMMENT '举报原因',
  detail VARCHAR(1000) DEFAULT NULL COMMENT '补充说明',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/handled/rejected',
  handler_id BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  handle_note VARCHAR(1000) DEFAULT NULL COMMENT '处理备注',
  handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_content_reports_status (status),
  KEY idx_content_reports_target (target_type, target_id),
  KEY idx_content_reports_reporter (reporter_id),
  KEY idx_content_reports_handler (handler_id),
  KEY idx_content_reports_created (created_at),
  CONSTRAINT fk_content_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_content_reports_handler FOREIGN KEY (handler_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容举报与审核表';

CREATE VIEW v_spot_search AS
SELECT
  s.id AS spot_id,
  COALESCE(s.representative_poi_id, s.legacy_poi_id) AS compat_poi_id,
  s.parent_id,
  s.name,
  s.short_name,
  s.spot_type,
  s.province,
  s.city,
  s.district,
  s.address,
  s.description,
  s.cover_image,
  s.longitude,
  s.latitude,
  s.rating,
  s.hotness,
  s.visit_count,
  s.status,
  GROUP_CONCAT(DISTINCT t.name ORDER BY t.name SEPARATOR ',') AS tag_names
FROM spots s
LEFT JOIN spot_tag_relations str ON str.spot_id = s.id
LEFT JOIN tags t ON t.id = str.tag_id
GROUP BY
  s.id, s.representative_poi_id, s.legacy_poi_id, s.parent_id, s.name,
  s.short_name, s.spot_type, s.province, s.city, s.district, s.address,
  s.description, s.cover_image, s.longitude, s.latitude, s.rating,
  s.hotness, s.visit_count, s.status;
