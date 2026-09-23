# 移民路线图（immigration-guide）

面向中文用户的**移民方式与流程指南**网站：按「国家 → 省/州 → 项目」三级结构梳理移民路径，提供结构化的申请条件、费用明细、办理周期、材料清单与分步流程，并内置基于规则打分的智能评估工具。

> ⚠️ **内容免责**：仓库中的种子内容基于公开常识整理，**上线前必须逐条人工核实**并更新每个项目文件的 `infoVerifiedAt` 字段；站点所有页面均展示免责声明，不构成法律意见。

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 框架 | Next.js 15（App Router，`output: "export"` 纯静态导出） | SEO 友好，可部署到任意静态托管 |
| 语言 | TypeScript（strict） | 内容与 UI 全量类型化 |
| 样式 | Tailwind CSS v4 | 无运行时依赖 |
| 内容校验 | zod（全部 `.strict()`） | 构建期快速失败，杜绝坏数据上线 |
| 内容存储 | JSON 文件（`content/`） | 内容即数据，未来可无痛换 headless CMS |

## 快速开始

```bash
npm install

npm run dev             # 开发服务器 http://localhost:3000
npm run content:check   # 单独跑内容体检（schema + 交叉引用校验）
npm run build           # 先内容体检，再构建并静态导出到 out/
npm start               # 本地预览静态产物（serve out）
```

改了 `content/` 下的文件后，`npm run dev` 需要重启才能生效（内容在模块加载时读取）。

## 目录结构

```
content/                    # ★ 全部站点内容（非代码）
  site.json                 #   站点名、标语、免责声明
  glossary.json             #   术语表
  countries/
    <country-id>/
      country.json          #   国家级信息（体系概述、联邦vs地方说明）
      regions/<region>.json #   省/州级信息（政策概述）
      programs/<slug>.json  #   移民项目（本站核心资产）
scripts/
  validate-content.ts       # 内容体检脚本
src/
  lib/
    schema.ts               # zod 数据模型（单一事实来源）
    content.ts              # 内容加载 + 交叉引用校验（服务端）
    ui.ts                   # 纯展示函数/样式映射（可进客户端）
    display.ts              # 服务端数据组装（Program → 卡片数据）
    assessment.ts           # 评估问卷与打分规则
  app/                      # 页面路由
    page.tsx                        # 首页（按国家 / 按 省·州 浏览入口）
    basics/                         # 移民科普（身份层级、方式逻辑、通用流程）
    countries/[country]/            # 国家页（联邦项目 + 省/州列表）
    countries/[country]/[region]/   # 省/州页（当地政策概述 + 该省项目）
    programs/[slug]/                # 项目详情页
    glossary/ about/                # 术语表 / 关于
    sitemap.ts robots.ts            # SEO
  components/               # UI 组件
public/flags/               # 国旗 SVG（Windows 不渲染旗帜类 emoji，故用本地图片）
```

## 容错性设计

1. **构建期内容校验**：`npm run build` 第一步就是 `content:check`——字段拼写错误（`.strict()` 拒绝未知字段）、缺失必填项、引用不存在的国家/省州、slug 重复、slug 与文件名不一致，全部一次性报告并中断构建。
2. **运行时兜底**：所有动态路由有 `notFound()` 兜底；`error.tsx` 全局错误边界提示排查方向；国旗图片缺失时回退为 emoji 文本。
3. **客户端/服务端边界清晰**：客户端组件只允许依赖 `lib/ui.ts`（纯函数），文件系统相关逻辑被隔离在 `lib/content.ts` / `lib/display.ts`。

## 内容原则

- **信息优先**：本站是纯信息站，不做打分、不做评估、不引导留资；给出事实与官方链接，用户自行判断。
- **只写可核实的内容**：不提供拍脑袋的「总费用 xx 万 / 周期 x 年」区间；费用只列可核实的官方规费与主要开销项，周期只标在各流程步骤上。
- **辖区全覆盖**：收录一个国家，就要覆盖其全部省/州/领地；不运作地方提名项目的辖区（如加拿大努纳武特）也要收录并说明实际情况。

## 可扩展性

- **新增国家/省州/项目 = 新增 JSON 文件**，零代码改动。
- `schema.ts` 是唯一的数据模型定义；要加新字段（如「多语言」「视频讲解」）先改 schema，校验器会强制所有存量内容补齐。

## 部署

`npm run build` 产出纯静态 `out/`，可部署到 GitHub Pages / Cloudflare Pages / Vercel / Nginx 等。自定义域名时设置环境变量：

```bash
NEXT_PUBLIC_SITE_URL=https://your-domain.com npm run build
```

（影响 `sitemap.xml` / `robots.txt` 中的绝对地址。）

## 路线图（第二阶段）

- [ ] 收藏/进度记录（本地存储，无需账号）
- [ ] 案例库、资料下载（PDF checklist）
- [ ] 内容后台（headless CMS 或本地 admin），支持多人协作维护
- [ ] 扩充国家（新西兰、英国、欧洲黄金签证、新加坡、日本等）

详细的内容维护流程见 [docs/内容维护指南.md](docs/内容维护指南.md)。
