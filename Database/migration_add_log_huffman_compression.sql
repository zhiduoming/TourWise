-- TourWise - 游记正文 Huffman 无损压缩字段
-- 说明：
-- - content 保留为明文字段，用于 MySQL FULLTEXT 检索和历史兼容。
-- - content_compressed 存储 Huffman 压缩后的二进制包。
-- - content_encoding 标记压缩算法。
-- - original/compressed size 用于答辩展示压缩率。

DROP PROCEDURE IF EXISTS add_travel_log_huffman_column;

DELIMITER //
CREATE PROCEDURE add_travel_log_huffman_column(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'travel_logs'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE travel_logs ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_travel_log_huffman_column(
    'content_compressed',
    'LONGBLOB DEFAULT NULL COMMENT ''Huffman压缩后的游记正文'' AFTER content'
);
CALL add_travel_log_huffman_column(
    'content_encoding',
    'VARCHAR(20) DEFAULT NULL COMMENT ''正文压缩算法，如 huffman-v1'' AFTER content_compressed'
);
CALL add_travel_log_huffman_column(
    'content_original_size',
    'INT DEFAULT NULL COMMENT ''原始正文UTF-8字节数'' AFTER content_encoding'
);
CALL add_travel_log_huffman_column(
    'content_compressed_size',
    'INT DEFAULT NULL COMMENT ''压缩后二进制字节数'' AFTER content_original_size'
);

DROP PROCEDURE IF EXISTS add_travel_log_huffman_column;
