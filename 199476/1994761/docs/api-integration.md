# Java HTTP API

前端统一通过 `src/api/http/httpAppApi.ts` 和 `httpFileApi.ts` 调用后端。除登录、注册、公开状态和游客反馈外，接口使用 `Authorization: Bearer <token>` 鉴权。

| 业务 | 接口 |
| --- | --- |
| 服务状态、业务快照 | `GET /api/health`、`GET /api/state` |
| 登录、注册、退出 | `POST /api/auth/login`、`POST /api/auth/register`、`POST /api/auth/logout` |
| 想法提交、公开、点赞、付款 | `POST /api/ideas`、`PATCH /api/ideas/:id/visibility`、`POST /api/ideas/:id/like`、`POST /api/ideas/:id/pay` |
| 管理员想法评估 | `POST /api/admin/ideas/:id/evaluate`、`PATCH /api/admin/ideas/:id/status` |
| 充值、套餐 | `POST /api/wallet/recharge`、`POST /api/packages/purchase` |
| 资料、密码、注销 | `PUT /api/profile`、`PUT /api/profile/password`、`DELETE /api/profile` |
| 反馈会话 | `POST /api/feedback`、`POST /api/feedback/:id/messages`、`POST /api/feedback/:id/close` |
| 通知 | `PATCH /api/notifications/:id/read`、`POST /api/notifications/read-all`、`POST /api/notifications/read-business`、`POST /api/notifications/derive` |
| 团队申请 | `PUT /api/team/application`、`PATCH /api/admin/team/:email/status` |
| 商务押金 | `POST /api/deposits`、`PATCH /api/admin/deposits/:id` |
| 文件 | `POST /api/files`、`GET /api/files/:id`、`DELETE /api/files/:id` |

接口错误统一返回：

```json
{ "ok": false, "error": "可直接展示给用户的错误信息" }
```

`GET /api/state` 会根据当前身份过滤数据：游客只得到公开想法，普通用户得到自己的完整业务数据，管理员得到审核工作台需要的全量数据。密码哈希永远不会返回前端。
