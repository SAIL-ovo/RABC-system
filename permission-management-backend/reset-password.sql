USE permission_management;

-- 重置 admin 密码为 admin123
UPDATE sys_user 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH' 
WHERE username = 'admin';
