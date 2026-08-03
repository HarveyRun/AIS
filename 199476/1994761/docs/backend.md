# 后端开发说明

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring MVC + Spring JDBC
- MySQL 8 / InnoDB / utf8mb4
- BCrypt 密码哈希
- 数据库会话 Token

## 核心源码

- `api/AppController.java`：全部业务 HTTP 路由。
- `api/FileController.java`：简历和交付文件上传、下载、删除。
- `service/AuthService.java`：注册、登录、BCrypt、Token 会话和管理员鉴权。
- `service/BusinessService.java`：想法、付款、套餐、反馈、通知、团队、押金等事务逻辑。
- `service/SnapshotService.java`：根据游客/用户/管理员身份组装前端业务快照。
- `config/DataSeeder.java`：仅在空库执行的演示数据初始化。
- `resources/schema.sql`：数据库关系模型和索引。

## 数据库

数据库名为 `diancheng`，项目账号只拥有该数据库权限。连接配置支持环境变量覆盖：

```text
DB_URL
DB_USER
DB_PASSWORD
SERVER_PORT
```

例如在 IDEA 中设置 `DB_PASSWORD` 后可覆盖 `application.yml` 的本地默认值。

## 修改和调试

使用 IntelliJ IDEA 打开 `backend/pom.xml`，运行 `DianchengApplication` 即可调试。前端通过 Vite `/api` 代理访问 8080，不需要修改 React 组件。

新增业务时，建议保持以下顺序：数据库表或字段 → `BusinessService` 事务 → `AppController` DTO/路由 → `httpAppApi.ts` 适配 → 页面交互与测试。
