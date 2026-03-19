-- ================================================================
-- HBase ActivityRecord 冷存储表创建脚本
-- ================================================================
-- 执行方式：通过 HBase Shell 或使用 hbase shell 命令执行
-- 确保命名空间 petcare_cold 已创建：create_namespace 'petcare_cold'

-- 1. 创建命名空间（如果不存在）
-- create_namespace 'petcare_cold'

-- 2. 创建活动记录冷存储表（与用户已创建的表结构一致）
create 'petcare_cold:activity_record',
  {NAME => 'info', VERSIONS => 1, COMPRESSION => 'GZ', BLOCKCACHE => 'true', TTL => '31536000'},
  {NAME => 'metadata', VERSIONS => 1, COMPRESSION => 'GZ', BLOCKCACHE => 'false', TTL => '31536000'},
  {NAME => 'statistics', VERSIONS => 1, COMPRESSION => 'GZ', BLOCKCACHE => 'false', TTL => '31536000'},
  {SPLITS => ['1', '2']}

-- ================================================================
-- HBase 表结构说明
-- ================================================================
--
-- RowKey 设计: {pet_id}_{date}_{activity_record_id}
-- 示例: 000000000001234_20250319_000000000987654
--
-- 列族 info:
--   - activity_record_id: 活动记录ID
--   - activity_id: 活动ID
--   - activity_name: 活动名称
--   - activity_description: 活动描述
--   - activity_date: 活动时间
--   - pet_id: 宠物ID
--   - user_id: 用户ID
--   - activity_kind_id: 活动种类ID
--   - activity_kind_name: 活动种类名称
--   - pet_name: 宠物名称
--
-- 列族 metadata:
--   - created_at: 创建时间
--   - last_access_time: 最后访问时间
--   - thaw_expire_time: 解冻过期时间（10分钟临时访问）
--   - migrated_at: 迁移时间
--
-- 列族 statistics:
--   - view_count: 浏览次数
--   - shared_count: 分享次数
--
-- ================================================================
-- 常用 HBase Shell 命令
-- ================================================================
--
-- 查看表结构: describe 'petcare_cold:activity_record'
--
-- 查看表列表: list 'petcare_cold:.*'
--
-- 统计行数: count 'petcare_cold:activity_record', INTERVAL => 100000
--
-- 启用表: enable 'petcare_cold:activity_record'
--
-- 禁用表: disable 'petcare_cold:activity_record'
--
-- 删除表: drop 'petcare_cold:activity_record'
--
-- 扫描前10行: scan 'petcare_cold:activity_record', LIMIT => 10
--
-- 获取特定行: get 'petcare_cold:activity_record', '000000000001234_20250319_000000000987654'
--
