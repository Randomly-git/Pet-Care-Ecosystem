-- 添加用户昵称和头像字段
ALTER TABLE user
ADD COLUMN nickname VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL';

-- 为现有用户设置默认昵称（如果没有设置的话）
UPDATE user SET nickname = name WHERE nickname IS NULL OR nickname = '';