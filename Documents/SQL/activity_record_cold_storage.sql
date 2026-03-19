-- ================================================================
-- ActivityRecord 冷热分离 - MySQL 表结构修改脚本
-- ================================================================
-- 执行前请备份数据库！

-- 1. 添加冷热分离相关字段
ALTER TABLE activity_record
ADD COLUMN IF NOT EXISTS storage_status VARCHAR(20) DEFAULT 'Hot' COMMENT '存储状态: Hot(热数据)/Cold(冷数据)',
ADD COLUMN IF NOT EXISTS storage_location VARCHAR(20) DEFAULT 'MYSQL' COMMENT '存储位置: MYSQL(热库)/HBASE(冷库)',
ADD COLUMN IF NOT EXISTS hbase_row_key VARCHAR(100) COMMENT 'HBase RowKey（迁移后记录）',
ADD COLUMN IF NOT EXISTS migration_status VARCHAR(20) DEFAULT 'NONE' COMMENT '迁移状态: NONE/PENDING/MIGRATING/MIGRATED/FAILED',
ADD COLUMN IF NOT EXISTS last_access_time DATETIME COMMENT '最后访问时间（用于判断是否需要从冷库解冻）',
ADD COLUMN IF NOT EXISTS migration_retry_count INT DEFAULT 0 COMMENT '迁移重试次数',
ADD COLUMN IF NOT EXISTS migration_error VARCHAR(500) COMMENT '迁移失败原因',
ADD COLUMN IF NOT EXISTS thaw_expire_time DATETIME COMMENT '解冻过期时间（10分钟后过期）',
ADD COLUMN IF NOT EXISTS is_thawing TINYINT(1) DEFAULT 0 COMMENT '是否正在解冻中';

-- 2. 添加索引（优化查询性能）
-- 迁移状态索引
ALTER TABLE activity_record
ADD INDEX IF NOT EXISTS idx_migration_status (migration_status);

-- 存储状态索引
ALTER TABLE activity_record
ADD INDEX IF NOT EXISTS idx_storage_status (storage_status);

-- 最后访问时间索引（用于解冻判断）
ALTER TABLE activity_record
ADD INDEX IF NOT EXISTS idx_last_access_time (last_access_time);

-- 复合索引（用于定时任务查询）
ALTER TABLE activity_record
ADD INDEX IF NOT EXISTS idx_activity_date_storage (activity_date, storage_status);

-- ================================================================
-- 注意：如果 MySQL 版本不支持 IF NOT EXISTS 子句，请分开执行
-- ================================================================

-- 分开执行的版本（兼容旧版 MySQL）：
-- ALTER TABLE activity_record ADD COLUMN storage_status VARCHAR(20) DEFAULT 'Hot' COMMENT '存储状态';
-- ALTER TABLE activity_record ADD COLUMN storage_location VARCHAR(20) DEFAULT 'MYSQL' COMMENT '存储位置';
-- ALTER TABLE activity_record ADD COLUMN hbase_row_key VARCHAR(100) COMMENT 'HBase RowKey';
-- ALTER TABLE activity_record ADD COLUMN migration_status VARCHAR(20) DEFAULT 'NONE' COMMENT '迁移状态';
-- ALTER TABLE activity_record ADD COLUMN last_access_time DATETIME COMMENT '最后访问时间';
-- ALTER TABLE activity_record ADD COLUMN migration_retry_count INT DEFAULT 0 COMMENT '迁移重试次数';
-- ALTER TABLE activity_record ADD COLUMN migration_error VARCHAR(500) COMMENT '迁移失败原因';
-- ALTER TABLE activity_record ADD COLUMN thaw_expire_time DATETIME COMMENT '解冻过期时间';
-- ALTER TABLE activity_record ADD COLUMN is_thawing TINYINT(1) DEFAULT 0 COMMENT '是否正在解冻中';

-- 添加索引
-- CREATE INDEX idx_migration_status ON activity_record(migration_status);
-- CREATE INDEX idx_storage_status ON activity_record(storage_status);
-- CREATE INDEX idx_last_access_time ON activity_record(last_access_time);
-- CREATE INDEX idx_activity_date_storage ON activity_record(activity_date, storage_status);
