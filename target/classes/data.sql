-- 插入默认模型
INSERT INTO models (name) VALUES 
('deepseek-coder:6.7b'),
('llama2'),
('codellama')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 添加默认管理员账号
INSERT INTO users (username, password, role) 
VALUES ('admin11', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 添加测试用户
INSERT INTO users (username, password, role) 
VALUES ('test', '123123', 'USER')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 添加测试团队
INSERT INTO teams (name, session_id) 
VALUES ('test', '03ad138e-ffc5-4168-bb1a-fd3f4cce29b5123')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 更新关系
UPDATE teams t 
SET t.leader_id = (SELECT id FROM users WHERE username = 'test' LIMIT 1)
WHERE t.name = 'test' AND NOT EXISTS (
    SELECT 1 FROM teams t2 WHERE t2.name = 'test' AND t2.leader_id IS NOT NULL
);

UPDATE users u 
SET u.team_id = (SELECT id FROM teams WHERE name = 'test' LIMIT 1)
WHERE u.username = 'test' AND u.team_id IS NULL;
