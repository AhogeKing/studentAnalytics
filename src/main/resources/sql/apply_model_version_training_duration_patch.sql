SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'model_version'
      AND COLUMN_NAME = 'training_duration_ms'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE model_version ADD COLUMN training_duration_ms BIGINT NULL COMMENT ''模型训练耗时毫秒'' AFTER trained_at',
    'SELECT ''training_duration_ms already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
