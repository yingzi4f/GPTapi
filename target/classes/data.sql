-- 插入默认模型
INSERT INTO models (name) VALUES 
('deepseek-coder:6.7b'),
('llama2'),
('codellama');

-- 添加默认管理员账号
INSERT INTO users (username, password, role) 
VALUES ('admin', 'admin123', 'ADMIN');

-- 添加测试用户
INSERT INTO users (username, password, role) 
VALUES ('test', '123123', 'USER');

-- 添加测试团队
INSERT INTO teams (name, session_id) 
VALUES ('test', '03ad138e-ffc5-4168-bb1a-fd3f4cce29b5123');

-- 更新关系
UPDATE teams t 
SET t.leader_id = (SELECT id FROM users WHERE username = 'test' LIMIT 1)
WHERE t.name = 'test';

UPDATE users u 
SET u.team_id = (SELECT id FROM teams WHERE name = 'test' LIMIT 1)
WHERE u.username = 'test';
