-- ================================================================
-- ActivityRecord 冷热分离 - MySQL 表结构修改脚本（简化版）
-- ================================================================
-- 执行前请备份数据库！
-- 更新说明：简化后的设计只需 migration_status 字段

-- 1. 添加冷热分离相关字段（简化版）
ALTER TABLE activity_record
ADD COLUMN IF NOT EXISTS migration_status VARCHAR(20) DEFAULT 'NONE' COMMENT '迁移状态: NONE/MIGRATING',
ADD COLUMN IF NOT EXISTS migration_retry_count INT DEFAULT 0 COMMENT '迁移重试次数',
ADD COLUMN IF NOT EXISTS migration_error VARCHAR(500) COMMENT '迁移失败原因';

-- 2. 添加索引
-- 迁移状态索引
ALTER TABLE activity_record
ADD INDEX IF NOT EXISTS idx_migration_status (migration_status);

-- ================================================================
-- 注意：如果 MySQL 版本不支持 IF NOT EXISTS 子句，请分开执行
-- ================================================================

-- 分开执行的版本（兼容旧版 MySQL）：
-- ALTER TABLE activity_record ADD COLUMN migration_status VARCHAR(20) DEFAULT 'NONE' COMMENT '迁移状态';
-- ALTER TABLE activity_record ADD COLUMN migration_retry_count INT DEFAULT 0 COMMENT '迁移重试次数';
-- ALTER TABLE activity_record ADD COLUMN migration_error VARCHAR(500) COMMENT '迁移失败原因';
-- ALTER TABLE activity_record ADD INDEX idx_migration_status (migration_status);

-- ================================================================
-- 回滚脚本（如果需要恢复）
-- ================================================================
-- ALTER TABLE activity_record DROP COLUMN IF EXISTS migration_status;
-- ALTER TABLE activity_record DROP COLUMN IF EXISTS migration_retry_count;
-- ALTER TABLE activity_record DROP COLUMN IF EXISTS migration_error;
-- ALTER TABLE activity_record DROP INDEX IF EXISTS idx_migration_status;
