# 事先问后端

正式后端位于前端项目根目录的 `backend/`，采用 Java 17、Spring Boot、Spring Data JPA、MySQL 和 Flyway。业务按认证、用户、答主、询问、钱包、通知、客服等领域拆分，不依赖前端模拟数据。

## 本地启动

1. 创建 MySQL 数据库 `shixianwen`，字符集使用 `utf8mb4`。
2. 在 `src/main/resources/application-local.yml` 中填写本机数据库密码。该文件已被 Git 忽略。
3. Windows 执行 `mvnw.cmd spring-boot:run`，其他系统执行 `./mvnw spring-boot:run`。
4. 服务默认地址为 `http://localhost:8080`，健康检查为 `/actuator/health`。

数据库表由 `src/main/resources/db/migration/` 自动管理。不要手工修改已经执行过的迁移；结构变更应新增下一版本迁移文件。

## 配置

生产环境通过环境变量提供数据库、跨域、文件存储和验证码服务配置。`application-local.yml` 仅供本机开发，不能提交密码、支付密钥或短信密钥。

### 经历证明压缩包直传

`STORAGE_PROVIDER=oss` 时，App 先向后端创建上传会话，再逐片取得 15 分钟有效的私有 OSS 预签名 URL，将文件分片直接上传到 OSS。全部上传后，后端向 OSS 核对分片数量、顺序、大小以及 ZIP/RAR 文件头；提交经历时只传一次性 `proofUploadId`，不会再把压缩包经由 Spring 转发。上传会话 24 小时内未绑定到经历会自动清理。

部署时须确保私有 Bucket 和服务端 OSS 账号允许分片上传、列举分片、完成/取消分片上传、读取对象元数据/文件头以及删除过期对象。长期 OSS 密钥只保留在后端，App 仅接收短期的单分片签名地址。更新后端时先让 Flyway 执行 `V122__experience_proof_direct_uploads.sql`，再安装支持直传的 App。旧客户端在 OSS 模式下通过 multipart 提交证明压缩包会被拒绝。

Spring multipart 上限已降为单文件 500MB、单请求 1GB，以兼容目前仍由应用服务器接收的严选认证录像等资料。`STORAGE_PROVIDER=local` 开发模式保留 500MB 内的旧上传方式；2GB 直传只在 OSS 模式下可用。

当前本地验证码由可替换配置提供，只用于开发联调。生产上线前需要接入真实短信服务。充值接口接入真实支付宝前，也必须配置支付宝应用与回调密钥，不能用前端成功提示代替支付结果通知。

## 主要业务状态

- 询问：`PENDING` → `ACTIVE` → `AWAITING_CONFIRMATION` → `COMPLETED`
- 询问也可进入：`REJECTED`、`CANCELLED`、`EXPIRED`
- 资金：发起时 `FROZEN`；拒绝、撤销、超时后 `REFUNDED`；确认结束后 `SETTLED`
- 待接受超过 24 小时自动退款；回答者申请结束后，提问者 48 小时未处理则自动结算。

所有资金变更都在事务中锁定钱包账户，并同步写入不可覆盖的账户流水。

## 验证

执行：

```powershell
./mvnw.cmd test
```

前端在项目根目录执行 `npm run build`。前端接口地址可通过 `VITE_API_BASE_URL` 覆盖，默认使用 `http://localhost:8080/api`。
