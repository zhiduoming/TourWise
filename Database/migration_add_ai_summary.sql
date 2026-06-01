USE tourist_system;

DROP PROCEDURE IF EXISTS add_poi_ai_summary_column;

DELIMITER //
CREATE PROCEDURE add_poi_ai_summary_column(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pois'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE pois ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_poi_ai_summary_column(
    'ai_summary',
    'TEXT DEFAULT NULL COMMENT ''AI生成的景点简介（DeepSeek缓存）'' AFTER description'
);
CALL add_poi_ai_summary_column(
    'ai_summary_at',
    'DATETIME DEFAULT NULL COMMENT ''AI简介生成时间'' AFTER ai_summary'
);

DROP PROCEDURE IF EXISTS add_poi_ai_summary_column;
