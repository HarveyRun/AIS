import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const appRoot = path.join(projectRoot, 'ssq');
const cssFiles = ['01-base.css', '06-overrides.css', '07-terminology.css', '08-layout.css', '09-result.css', '10-refinements.css', '11-probability-rank.css'];
const jsFiles = ['01-app.js', '06-overrides.js', '07-terminology.js', '08-layout.js', '09-result.js', '10-refinements.js', '11-probability-rank.js'];

const snapshot = await readFile(path.join(appRoot, 'legacy', 'index.monolith.html'), 'utf8');
const baselineHtml = await readFile(path.join(appRoot, 'baseline.html'), 'utf8');
const reactIndexHtml = await readFile(path.join(appRoot, 'index.html'), 'utf8');
const originalStyles = [...snapshot.matchAll(/<style([^>]*)>([\s\S]*?)<\/style>/gi)];
const originalScripts = [...snapshot.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/gi)];

if (/<style(?:\s|>)/i.test(baselineHtml)) throw new Error('baseline.html 仍包含内联 style');
if (/<script(?![^>]*\bsrc=)[^>]*>/i.test(baselineHtml)) throw new Error('baseline.html 仍包含内联 script');
if (originalStyles.length !== 7 || originalScripts.length !== 7) throw new Error('原始快照标签数量异常');

let reconstructed = baselineHtml;
for (let index = 0; index < cssFiles.length; index += 1) {
  const file = cssFiles[index];
  const content = await readFile(path.join(appRoot, 'assets', 'css', file), 'utf8');
  if (content !== originalStyles[index][2]) throw new Error(`${file} 与原页面不一致`);
  reconstructed = reconstructed.replace(`<link rel="stylesheet" href="./assets/css/${file}">`, () => originalStyles[index][0]);
}

const dataSource = await readFile(path.join(appRoot, 'data', 'forecast-data.js'), 'utf8');
const appSource = await readFile(path.join(appRoot, 'assets', 'js', jsFiles[0]), 'utf8');
if (`${dataSource}${appSource}` !== originalScripts[0][2]) throw new Error('DATA 与主脚本无法还原原页面');
reconstructed = reconstructed.replace(
  '<script src="./data/forecast-data.js"></script>\n<script src="./assets/js/01-app.js"></script>',
  () => originalScripts[0][0],
);

for (let index = 1; index < jsFiles.length; index += 1) {
  const file = jsFiles[index];
  const content = await readFile(path.join(appRoot, 'assets', 'js', file), 'utf8');
  if (content !== originalScripts[index][2]) throw new Error(`${file} 与原页面不一致`);
  const id = /\bid="([^"]+)"/.exec(originalScripts[index][1])?.[1];
  reconstructed = reconstructed.replace(
    `<script${id ? ` id="${id}"` : ''} src="./assets/js/${file}"></script>`,
    () => originalScripts[index][0],
  );
}

if (reconstructed !== snapshot) throw new Error('外部资源无法完整还原原页面');

const dataJsonSource = dataSource.slice('const DATA='.length, dataSource.endsWith(';') ? -1 : undefined);
const data = JSON.parse(dataJsonSource);
const reactDataSource = await readFile(path.join(appRoot, 'src', 'data', 'forecast-data.json'), 'utf8');
const reactData = JSON.parse(reactDataSource);
if (JSON.stringify(reactData) !== JSON.stringify(data)) throw new Error('React 数据模块与原 DATA 不一致');

const originalBody = /<body>([\s\S]*?)<script>const DATA=/i.exec(snapshot)?.[1];
const reactBody = await readFile(path.join(appRoot, 'src', 'legacy', 'body.html'), 'utf8');
if (!originalBody || reactBody !== originalBody) throw new Error('React 页面模板与原 body 不一致');

const runtime = await readFile(path.join(appRoot, 'src', 'legacy', 'bootLegacy.js'), 'utf8');
if (!runtime.includes('export function bootLegacyRuntime(DATA)')) throw new Error('React 兼容运行时缺少受控入口');
if (!runtime.includes('window.__drawVisibleCharts = drawVisibleCharts')) throw new Error('React 兼容运行时未暴露图表重绘入口');
for (const forbidden of ['function gotoPage(', 'function historyNav(', 'function profTabs(', "$$('.nav button').forEach"]) {
  if (runtime.includes(forbidden)) throw new Error(`旧脚本仍在接管 React 导航：${forbidden}`);
}

if (!reactIndexHtml.includes('<div id="root"></div>') || !reactIndexHtml.includes('src="/src/main.tsx"')) {
  throw new Error('React 入口不完整');
}

for (const file of [
  ...cssFiles.map((name) => path.join('assets', 'css', name)),
  ...jsFiles.map((name) => path.join('assets', 'js', name)),
  path.join('data', 'forecast-data.js'),
  path.join('src', 'App.tsx'),
  path.join('src', 'features', 'algorithm', 'AlgorithmPage.tsx'),
  path.join('src', 'features', 'algorithm', 'coreAlgorithm.ts'),
  path.join('src', 'navigation', 'NavigationContext.tsx'),
  path.join('src', 'legacy', 'LegacyDocument.tsx'),
  path.join('src', 'legacy', 'LegacyRuntimeBridge.tsx'),
]) {
  await access(path.join(appRoot, file));
}

for (const file of jsFiles) {
  const content = await readFile(path.join(appRoot, 'assets', 'js', file), 'utf8');
  new Function(content);
}
new Function(dataSource);

if (data.probabilities.red.length !== 33 || data.probabilities.blue.length !== 16) {
  throw new Error('概率号码数量不完整');
}

console.log(JSON.stringify({
  forecastIssue: data.meta.forecastIssue,
  drawCount: data.meta.drawCount,
  cssFiles: cssFiles.length,
  scriptFiles: jsFiles.length,
  redProbabilities: data.probabilities.red.length,
  blueProbabilities: data.probabilities.blue.length,
  exactReconstruction: true,
  exactReactTemplate: true,
  exactReactData: true,
  reactOwnsNavigation: true,
}, null, 2));
