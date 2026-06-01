-- 路网版本快照表
-- 用法：在 IDEA Database Console 连接 tourist_system 后执行本文件。

USE tourist_system;

CREATE TABLE IF NOT EXISTS route_graph_versions (
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
