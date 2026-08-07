# 光忆移动端

基于 React 与 Vite 的手机端前端项目。

## 开发

```bash
npm install
npm run dev
```

## 检查与构建

```bash
npm run check
npm run build
```

## 目录约定

```text
src/
├─ app/                  应用组合、跨页面共享状态
├─ routes/               正式路由配置与页面路由声明
│  ├─ routeConfig.js     路径常量
│  └─ AppRoutes.jsx      React Router 页面映射
├─ pages/                一个页面一个 JSX 文件
│  ├─ auth/              LoginPage、RegisterPage
│  ├─ home/              HomePage
│  ├─ discovery/         KnowledgePage、FilterPage、FilteredTalentPage
│  ├─ talent/            TalentPage
│  ├─ items/             ApplyPage、MyItemsPage、CreateMatterPage、MatterPage
│  ├─ messages/          MessagesPage、GroupChatPage
│  ├─ profile/           ProfilePage、SettingsPage、WalletPage
│  ├─ certification/     认证入口、基础信息、材料上传页面
│  ├─ notifications/     NoticesPage
│  ├─ rules/             RulesPage
│  └─ support/           RatingPage、FeedbackPage
├─ components/           可复用布局和导航
├─ hooks/                可复用状态逻辑
├─ data/                 模拟数据
├─ styles/               仅保留跨页面公共样式
└─ main.jsx              React 启动入口
```

## 约束

- `main.jsx` 不允许放业务逻辑。
- 每个页面必须是独立 JSX 文件，禁止重新创建 `*Pages.jsx` 聚合业务文件。
- 页面专属样式与页面放在同一目录，不进入公共样式。
- 所有 URL 与路由声明只允许放在 `routes/`。
- 跨页面共享状态由 `app/` 组合。
- 可复用交互逻辑放入 `hooks/`。
- 可复用视觉结构放入 `components/`。
- 全局样式只从 `styles/index.css` 引入。
