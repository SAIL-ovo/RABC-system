CREATE DATABASE IF NOT EXISTS permission_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE permission_management;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    department_id BIGINT DEFAULT NULL,
    post_id BIGINT DEFAULT NULL,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    data_scope VARCHAR(30) DEFAULT 'ALL',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    url VARCHAR(200),
    method VARCHAR(10),
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    type INT DEFAULT 1,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT DEFAULT 0,
    leader_user_id BIGINT DEFAULT NULL,
    leader VARCHAR(50),
    phone VARCHAR(20),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    department_id BIGINT DEFAULT NULL,
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_post_role (
    post_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, role_id),
    FOREIGN KEY (post_id) REFERENCES sys_post(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS department_id BIGINT DEFAULT NULL;

ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS post_id BIGINT DEFAULT NULL;

ALTER TABLE sys_post
ADD COLUMN IF NOT EXISTS department_id BIGINT DEFAULT NULL;

ALTER TABLE sys_department
ADD COLUMN IF NOT EXISTS leader_user_id BIGINT DEFAULT NULL;

ALTER TABLE sys_role
ADD COLUMN IF NOT EXISTS data_scope VARCHAR(30) DEFAULT 'ALL';

UPDATE sys_role
SET data_scope = 'ALL'
WHERE data_scope IS NULL OR data_scope = '';

INSERT INTO sys_user (username, password, real_name, email, phone, status)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Administrator', 'admin@example.com', '13800138000', 1);

INSERT INTO sys_role (name, code, description, status)
VALUES
    ('Administrator', 'ADMIN', 'System administrator', 1),
    ('User', 'USER', 'Regular user', 1);

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
VALUES
    ('User Management', 'user:manage', '/api/user', 'GET', 0, 1, 'User', 1, 1, 1),
    ('Role Management', 'role:manage', '/api/role', 'GET', 0, 1, 'Operation', 2, 1, 1),
    ('Permission Management', 'permission:manage', '/api/permission', 'GET', 0, 1, 'Lock', 3, 1, 1);

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
VALUES
    ('新增用户', 'user:create', NULL, NULL, 1, 2, NULL, 11, 2, 1),
    ('编辑用户', 'user:update', NULL, NULL, 1, 2, NULL, 12, 2, 1),
    ('删除用户', 'user:delete', NULL, NULL, 1, 2, NULL, 13, 2, 1),
    ('分配用户角色', 'user:assign-role', NULL, NULL, 1, 2, NULL, 14, 2, 1),
    ('重置用户密码', 'user:reset-password', NULL, NULL, 1, 2, NULL, 15, 2, 1),
    ('切换用户状态', 'user:status', NULL, NULL, 1, 2, NULL, 16, 2, 1),
    ('新增角色', 'role:create', NULL, NULL, 2, 2, NULL, 21, 2, 1),
    ('编辑角色', 'role:update', NULL, NULL, 2, 2, NULL, 22, 2, 1),
    ('删除角色', 'role:delete', NULL, NULL, 2, 2, NULL, 23, 2, 1),
    ('分配角色权限', 'role:assign-permission', NULL, NULL, 2, 2, NULL, 24, 2, 1),
    ('切换角色状态', 'role:status', NULL, NULL, 2, 2, NULL, 25, 2, 1),
    ('新增权限', 'permission:create', NULL, NULL, 3, 2, NULL, 31, 2, 1),
    ('编辑权限', 'permission:update', NULL, NULL, 3, 2, NULL, 32, 2, 1),
    ('删除权限', 'permission:delete', NULL, NULL, 3, 2, NULL, 33, 2, 1);

INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4),
    (1, 5),
    (1, 6),
    (1, 7),
    (1, 8),
    (1, 9),
    (1, 10),
    (1, 11),
    (1, 12),
    (1, 13),
    (1, 14),
    (1, 15),
    (1, 16),
    (1, 17);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'Operator user id',
    username VARCHAR(50) COMMENT 'Operator username',
    operation VARCHAR(100) COMMENT 'Operation description',
    method VARCHAR(200) COMMENT 'Method name',
    params TEXT COMMENT 'Request params',
    ip VARCHAR(50) COMMENT 'Operator IP',
    status INT DEFAULT 1 COMMENT '1 success, 0 failed',
    error_message TEXT COMMENT 'Error message',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
);

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '操作日志', 'log:view', NULL, NULL, 0, 1, 'Document', 4, 1, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'log:view'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.code = 'log:view'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = 1
        AND rp.permission_id = p.id
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '查看用户', 'user:view', NULL, NULL, 1, 2, NULL, 10, 2, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'user:view'
);

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '查看角色', 'role:view', NULL, NULL, 2, 2, NULL, 20, 2, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'role:view'
);

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '查看权限', 'permission:view', NULL, NULL, 3, 2, NULL, 30, 2, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'permission:view'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.code IN ('user:view', 'role:view', 'permission:view')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = 1
        AND rp.permission_id = p.id
  );

UPDATE sys_permission
SET name = '操作日志',
    url = '/home/operation-log',
    parent_id = 0,
    level = 1,
    icon = 'Document',
    sort = 4,
    type = 1,
    status = 1,
    deleted = 0
WHERE code = 'log:view';

-- 标准测试角色：用于稳定验证查看权限、写权限和菜单展示是否符合预期
INSERT INTO sys_role (name, code, description, status)
SELECT '用户只读', 'USER_VIEWER', '只能查看用户列表，不能执行新增、编辑、删除等操作', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE code = 'USER_VIEWER'
);

INSERT INTO sys_role (name, code, description, status)
SELECT '角色只读', 'ROLE_VIEWER', '只能查看角色列表，不能执行新增、编辑、删除或分配权限', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE code = 'ROLE_VIEWER'
);

INSERT INTO sys_role (name, code, description, status)
SELECT '权限只读', 'PERMISSION_VIEWER', '只能查看权限列表和权限树，不能维护权限数据', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE code = 'PERMISSION_VIEWER'
);

INSERT INTO sys_role (name, code, description, status)
SELECT '日志只读', 'LOG_VIEWER', '只能查看操作日志，用于验证审计权限隔离', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE code = 'LOG_VIEWER'
);

INSERT INTO sys_role (name, code, description, status)
SELECT '系统只读', 'SYSTEM_VIEWER', '可以查看用户、角色、权限和操作日志，但不能做任何写操作', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE code = 'SYSTEM_VIEWER'
);

-- 用户只读：只授予 user:view
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = 'user:view'
WHERE r.code = 'USER_VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 角色只读：只授予 role:view
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = 'role:view'
WHERE r.code = 'ROLE_VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 权限只读：只授予 permission:view
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = 'permission:view'
WHERE r.code = 'PERMISSION_VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 日志只读：只授予 log:view
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = 'log:view'
WHERE r.code = 'LOG_VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 系统只读：统一授予四个模块的查看权限，便于做全局只读回归测试
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('user:view', 'role:view', 'permission:view', 'log:view')
WHERE r.code = 'SYSTEM_VIEWER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 部门管理：顶级菜单和细粒度操作权限
INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '部门管理', 'department:manage', '/home/department', 'GET', 0, 1, 'OfficeBuilding', 4, 1, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'department:manage'
);

UPDATE sys_permission
SET sort = 5
WHERE code = 'log:view';

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '查看部门', 'department:view', NULL, NULL, p.id, 2, NULL, 40, 2, 1
FROM sys_permission p
WHERE p.code = 'department:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'department:view'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '新增部门', 'department:create', NULL, NULL, p.id, 2, NULL, 41, 2, 1
FROM sys_permission p
WHERE p.code = 'department:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'department:create'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '编辑部门', 'department:update', NULL, NULL, p.id, 2, NULL, 42, 2, 1
FROM sys_permission p
WHERE p.code = 'department:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'department:update'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '删除部门', 'department:delete', NULL, NULL, p.id, 2, NULL, 43, 2, 1
FROM sys_permission p
WHERE p.code = 'department:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'department:delete'
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.code IN ('department:manage', 'department:view', 'department:create', 'department:update', 'department:delete')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = 1
        AND rp.permission_id = p.id
  );

-- 岗位管理：顶级菜单和细粒度操作权限
INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '宀椾綅绠＄悊', 'post:manage', '/home/post', 'GET', 0, 1, 'Briefcase', 5, 1, 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE code = 'post:manage'
);

UPDATE sys_permission
SET sort = 6
WHERE code = 'log:view';

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '鏌ョ湅宀椾綅', 'post:view', NULL, NULL, p.id, 2, NULL, 50, 2, 1
FROM sys_permission p
WHERE p.code = 'post:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'post:view'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '鏂板宀椾綅', 'post:create', NULL, NULL, p.id, 2, NULL, 51, 2, 1
FROM sys_permission p
WHERE p.code = 'post:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'post:create'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '缂栬緫宀椾綅', 'post:update', NULL, NULL, p.id, 2, NULL, 52, 2, 1
FROM sys_permission p
WHERE p.code = 'post:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'post:update'
  );

INSERT INTO sys_permission (name, code, url, method, parent_id, level, icon, sort, type, status)
SELECT '鍒犻櫎宀椾綅', 'post:delete', NULL, NULL, p.id, 2, NULL, 53, 2, 1
FROM sys_permission p
WHERE p.code = 'post:manage'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission
      WHERE code = 'post:delete'
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.code IN ('post:manage', 'post:view', 'post:create', 'post:update', 'post:delete')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = 1
        AND rp.permission_id = p.id
  );

-- 内置几个演示岗位，方便岗位管理页和用户绑定页开箱即用
INSERT INTO sys_post (name, code, sort, status, remark)
SELECT '鎬荤粡鐞?', 'GENERAL_MANAGER', 1, 1, '璐熻矗鏁翠綋缁勭粐鍗忚皟涓庨噸鐐瑰喅绛?'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_post
    WHERE code = 'GENERAL_MANAGER'
);

INSERT INTO sys_post (name, code, sort, status, remark)
SELECT '浜嬪姟涓撳憳', 'HR_SPECIALIST', 2, 1, '璐熻矗浜嬪姟绠＄悊鍜屼汉鍛樻湇鍔′簨椤?'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_post
    WHERE code = 'HR_SPECIALIST'
);

INSERT INTO sys_post (name, code, sort, status, remark)
SELECT '璐㈠姟涓撳憳', 'FINANCE_SPECIALIST', 3, 1, '璐熻矗璐㈠姟缁熻鍜岀洊绔犳祦杞?'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_post
    WHERE code = 'FINANCE_SPECIALIST'
);
