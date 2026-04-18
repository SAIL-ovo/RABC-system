# 权限与组织管理系统

基于 `Spring Boot + Spring Security + JWT + Redis + Vue 3 + Element Plus + MyBatis-Plus` 搭建的企业级权限与组织管理系统。

项目不只是基础的 RBAC 后台，还围绕 `部门、岗位、用户、角色、数据权限、操作审计、首页仪表盘` 构建了较完整的组织授权模型，适合用于权限系统学习、课程设计、毕业设计和简历项目展示。

## 项目亮点

- 实现基于 `RBAC + 数据范围` 的权限模型，支持 `全部数据 / 本部门及下级 / 仅本部门 / 仅本人`
- 实现 `部门 + 岗位 + 用户 + 角色` 的组织模型
- 支持岗位归属部门、岗位默认角色模板、用户岗位联动授权
- 基于 `Spring Security + JWT + Redis` 实现登录鉴权和退出后 token 立即失效
- 支持用户管理、角色管理、权限管理、部门管理、岗位管理、操作日志、首页概览
- 支持权限变更留痕、操作日志筛选与导出
- 支持首页仪表盘展示组织规模、权限治理亮点、最近动态和近 7 天审计趋势

## 功能模块

### 1. 登录与认证

- 用户登录
- JWT 鉴权
- 登录状态恢复
- 退出登录后 token 进入 Redis 黑名单
- 修改当前用户密码

### 2. 用户管理

- 用户分页查询
- 新增、编辑、删除用户
- 启用 / 停用用户
- 重置密码
- 分配角色
- 查看用户组织画像

### 3. 角色管理

- 角色分页查询
- 新增、编辑、删除角色
- 角色状态管理
- 分配角色权限
- 配置角色数据范围

### 4. 权限管理

- 菜单权限管理
- 按钮权限管理
- 接口权限管理
- 权限树展示

### 5. 部门管理

- 部门树管理
- 新增、编辑、删除部门
- 配置部门负责人

### 6. 岗位管理

- 岗位分页查询
- 新增、编辑、删除岗位
- 岗位归属部门
- 岗位默认角色模板
- 统计岗位绑定用户数

### 7. 数据权限

- 用户列表按当前登录人数据范围过滤
- 支持角色维度配置数据范围
- 首页展示当前支持的数据权限类型

### 8. 审计与日志

- 操作日志分页查询
- 按用户、模块、状态、关键字筛选
- 权限变更 / 用户变更 / 组织变更 / 安全操作分类查看
- 操作日志导出

### 9. 首页概览

- 用户总数
- 角色总数
- 部门总数
- 岗位总数
- 权限总数
- 审计日志总数
- 近 7 天审计趋势
- 最近动态
- 授权治理亮点

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- JWT
- Maven

### 前端

- Vue 3
- Vue Router
- Vuex
- Axios
- Element Plus
- Vite

## 项目结构

```text
E:\the office system
├─ permission-management-backend
│  ├─ src/main/java/com/example
│  │  ├─ config          # 安全、Redis 等配置
│  │  ├─ controller      # 控制器
│  │  ├─ entity          # 实体类
│  │  ├─ filter          # JWT 过滤器
│  │  ├─ mapper          # MyBatis-Plus Mapper
│  │  ├─ service         # 业务服务
│  │  └─ utils           # JWT 等工具类
│  └─ src/main/resources
│     ├─ application.yml # 数据源、Redis 等配置
│     └─ init.sql        # 初始化脚本
│
└─ permission-management-frontend
   ├─ src
   │  ├─ components      # 布局组件
   │  ├─ router          # 路由配置
   │  ├─ store           # 全局状态
   │  ├─ utils           # 请求封装
   │  └─ views           # 页面视图
   └─ package.json
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8.x
- Redis 6.x / 7.x

## 数据库与缓存配置

后端默认配置位于：

- `permission-management-backend/src/main/resources/application.yml`

当前项目默认使用：

- MySQL：`localhost:3306/permission_management`
- Redis：`localhost:6379`
- 后端端口：`8080`

请根据自己的本地环境修改数据库账号、密码和 Redis 配置。

## 初始化项目

### 1. 初始化数据库

1. 创建数据库：

```sql
CREATE DATABASE permission_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

2. 执行初始化脚本：

- `permission-management-backend/src/main/resources/init.sql`

### 2. 启动后端

进入后端目录：

```bash
cd permission-management-backend
```

启动项目：

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package
java -jar target/*.jar
```

### 3. 启动前端

进入前端目录：

```bash
cd permission-management-frontend
```

安装依赖：

```bash
npm install
```

启动开发环境：

```bash
npm run dev
```

生产构建：

```bash
npm run build
```

## 默认账号

初始化脚本内置管理员账号：

- 用户名：`admin`
- 密码：`123456`

如果你修改过初始化脚本或数据库，请以你本地数据为准。

## 关键设计说明

### 1. 菜单权限

左侧菜单并不是写死在页面里，而是结合：

- 前端路由配置
- 当前用户权限集合
- 后端返回菜单信息

共同生成。

### 2. 数据权限

角色支持配置数据范围：

- `ALL`
- `DEPT_AND_CHILD`
- `DEPT`
- `SELF`

当前版本主要应用在用户列表的数据过滤。

### 3. Redis 黑名单

用户退出登录时，当前 JWT 会写入 Redis 黑名单。

这样即使 token 还没自然过期，只要用户已经退出，后续请求也会被后端拒绝。

### 4. 岗位与组织联动

- 岗位必须归属于某个部门
- 用户选择部门后，只能选择该部门下的岗位
- 岗位可以配置默认角色模板
- 用户选择岗位后可自动带出岗位默认角色

## 简历可写亮点

你可以将该项目描述为：

> 独立完成企业级权限与组织管理系统的前后端开发，基于 Spring Boot、Spring Security、JWT、Redis 和 Vue 3 实现 RBAC 权限控制、数据权限、岗位默认角色、部门岗位联动、操作审计与首页仪表盘等核心能力。

建议简历关键词：

- RBAC 权限模型
- 数据权限
- Spring Security
- JWT
- Redis 黑名单
- 组织模型
- 操作审计
- Vue 3 后台管理系统

## 后续可扩展方向

- 登录失败限制
- 验证码
- 多租户
- 审批式授权
- 数据权限扩展到更多业务模块
- CI/CD 与自动化部署

