# 点成全栈项目

点成现在是一个完整的前后端分离项目：React + TypeScript 单页前端、Spring Boot 3 Java 后端、MySQL 8 数据库。开发与生产运行时全部调用真实 HTTP API；Mock 只在前端自动化测试中使用。

## 当前本机配置

- 前端：http://127.0.0.1:5173
- Java API：http://127.0.0.1:8080/api
- MySQL：127.0.0.1:3306 / `diancheng`
- 数据库账号：`diancheng_app`（仅拥有 `diancheng.*` 权限）
- 普通演示账号：`demo01@diancheng.test` / `12345678`
- 管理员账号：`timeline.1994.1976@gmail.com` / `12345678`
- 主邀请码：`DC-199476`

## 一键启动

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-all.ps1
```

只启动 Java 后端：

```powershell
npm run backend:start
```

只启动前端：

```powershell
npm run dev
```

停止 Java 后端：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/stop-backend.ps1
```

## 项目结构

```text
backend/                         # 完整 Java 后端源码，可直接用 IDEA 打开
├─ pom.xml                       # Maven / Spring Boot 项目入口
└─ src/main/
   ├─ java/com/diancheng/
   │  ├─ api/                    # HTTP Controller 与统一错误处理
   │  ├─ config/                 # CORS、演示数据初始化
   │  └─ service/                # 登录鉴权、业务事务、快照查询
   └─ resources/
      ├─ application.yml         # 数据库与服务配置
      └─ schema.sql              # MySQL 完整关系表结构
src/
├─ api/http/                     # 真实 HTTP AppApi / FileApi 适配器
├─ api/mock/                     # 仅用于前端测试的 Mock 实现
├─ application/                  # 状态、导航与应用服务
├─ domain/                       # TypeScript 领域模型
├─ features/                     # 个人中心和业务模块
├─ pages/                        # 单页路由页面
└─ styles/                       # 全局与页面级样式
scripts/                         # Windows 本地启动、停止脚本
docs/                            # 业务、API 与迁移核对文档
```

## 构建与检查

```powershell
npm run lint
npm test
npm run build
npm run backend:build
```

后端首次启动会自动执行 `schema.sql` 建表，并在空数据库中写入演示账号和必要业务数据。已有数据不会被覆盖。

更多说明见 [后端开发文档](docs/backend.md)、[接口文档](docs/api-integration.md) 和 [业务边界](docs/business.md)。
