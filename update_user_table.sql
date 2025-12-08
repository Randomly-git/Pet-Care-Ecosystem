-- 更新用户表结构，添加社区功能需要的字段
-- 执行前请备份数据库！

-- 添加昵称字段
ALTER TABLE user
ADD COLUMN nickname VARCHAR(100) DEFAULT NULL COMMENT '用户昵称'
AFTER name;

-- 添加头像字段
ALTER TABLE user
ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL'
AFTER nickname;

-- 为现有用户设置默认昵称（使用name作为nickname）
UPDATE user SET nickname = name WHERE nickname IS NULL OR nickname = '';

-- 验证更新结果
SELECT user_id, name, nickname, avatar_url FROM user LIMIT 5;