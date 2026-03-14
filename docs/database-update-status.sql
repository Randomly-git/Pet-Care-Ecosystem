-- 宠物状态表结构更新脚本
-- 将 status 表从用户关联改为宠物关联，并添加 state 和 status_value 字段

-- 1. 先备份旧数据（可选）
-- CREATE TABLE status_backup AS SELECT * FROM status;

-- 2. 添加新字段
ALTER TABLE status ADD COLUMN pet_id BIGINT AFTER status_id;
ALTER TABLE status ADD COLUMN state INT DEFAULT 1 AFTER status_value;
ALTER TABLE status ADD COLUMN status_value VARCHAR(255) AFTER status_name;

-- 3. 将 user_id 的数据迁移到 pet_id（如果有 pet 表的话）
-- 这里假设原来宠物和用户的关联可以通过其他方式获取
-- 如果你有 pet 表，可以执行类似：
-- UPDATE status s 
-- INNER JOIN pet p ON s.user_id = p.user_id 
-- SET s.pet_id = p.pet_id;

-- 4. 如果没有 pet 表，需要手动将已有的 status 数据关联到宠物
-- 假设你有 pet 表，可以通过以下方式迁移（如果没有 pet 表，需要先创建）
-- UPDATE status s INNER JOIN pet p ON s.user_id = p.user_id SET s.pet_id = p.pet_id;

-- 5. 删除旧的 user_id 字段（确认数据迁移完成后）
ALTER TABLE status DROP COLUMN user_id;

-- 6. 设置 pet_id 为非空（如果已有数据的话）
-- ALTER TABLE status MODIFY COLUMN pet_id BIGINT NOT NULL;

-- 7. 添加外键约束（可选）
-- ALTER TABLE status ADD CONSTRAINT fk_status_pet FOREIGN KEY (pet_id) REFERENCES pet(pet_id);

-- 查看更新后的表结构
-- DESCRIBE status;
