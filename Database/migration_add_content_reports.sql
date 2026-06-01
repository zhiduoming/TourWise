-- 内容举报与审核模块
-- 执行方式：在 IDEA Database Console 中运行本文件。

USE tourist_system;

SET @has_log_comment_status := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'log_comments'
    AND COLUMN_NAME = 'status'
);

SET @add_log_comment_status := IF(
  @has_log_comment_status = 0,
  'ALTER TABLE log_comments ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态: 1正常 0已删除'' AFTER content',
  'SELECT ''log_comments.status already exists'''
);

PREPARE stmt FROM @add_log_comment_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS content_reports (
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
