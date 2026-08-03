# 智选：双色球预测

这是原双色球预测 Demo 的原位 React 工程化版本。业务数据、计算结果、DOM 顺序、CSS 加载顺序和页面交互保持原样；React 负责应用入口、组件树以及主导航、历史子导航和专业说明子导航的状态。

## 本地运行

```bash
npm install
npm run dev
```

生产检查和构建：

```bash
npm run typecheck
npm run test
npm run verify
npm run build
npm run preview
```

## 目录结构

```text
ssq/
  index.html                         Vite / React 入口
  baseline.html                      原页面拆分后的逐字节对照入口
  assets/css/                        原有 7 层样式，加载顺序不变
  assets/js/                         经校验的原始业务实现
  data/forecast-data.js              原 DATA 数据源
  legacy/index.monolith.html         重构前的完整页面快照
  src/
    App.tsx                           应用组合根节点
    main.tsx                          React 挂载和数据异步加载
    features/algorithm/               核心算法说明页及组合评分实现
    navigation/NavigationContext.tsx React 导航状态
    legacy/LegacyDocument.tsx        原 DOM 到 React 组件边界的映射
    legacy/LegacyRuntimeBridge.tsx   原业务渲染器的受控适配层
    legacy/body.html                 自动生成的原页面模板
    legacy/bootLegacy.js             自动生成的兼容运行时
    data/forecast-data.json          自动生成的 React 数据模块
scripts/
  generate-react-runtime.mjs         生成 React 模板、数据和兼容层
  verify-ssq.mjs                     校验原版及 React 版的一致性
```

## 架构约定

- 新功能从 React 组件和状态层进入，不在 `body.html` 中直接追加事件。
- `body.html`、`bootLegacy.js` 和 `forecast-data.json` 是生成文件，不直接修改。
- 旧业务脚本只通过 `LegacyRuntimeBridge` 启动，不能重新接管页面导航。
- 每次迁移一个旧渲染器时，先保持原 DOM 和 class，再用 React 输出替换对应的兼容逻辑。
- `baseline.html` 和 `index.monolith.html` 只用于回归、校验和恢复，不作为正式入口。

## 一致性校验

`npm run verify` 会检查：

- 7 个 CSS 和 7 个业务脚本可以逐字节还原原始页面。
- React 使用的模板和数据与原页面完全一致。
- React 兼容层没有保留旧的主导航、历史导航和专业说明导航处理器。
- 预测期号、历史开奖数量、33 个红球概率和 16 个蓝球概率完整。

这套结构是兼容优先的 React 迁移：界面和业务先保持不动，后续迭代可以按页面逐步把兼容渲染器替换为类型化 React 组件，而不需要再重写整站。
