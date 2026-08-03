import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const appRoot = path.join(projectRoot, 'ssq');
const indexPath = path.join(appRoot, 'index.html');
const legacyDir = path.join(appRoot, 'legacy');
const snapshotPath = path.join(legacyDir, 'index.monolith.html');
const cssDir = path.join(appRoot, 'assets', 'css');
const jsDir = path.join(appRoot, 'assets', 'js');
const dataDir = path.join(appRoot, 'data');

const cssFiles = [
  '01-base.css',
  '06-overrides.css',
  '07-terminology.css',
  '08-layout.css',
  '09-result.css',
  '10-refinements.css',
  '11-probability-rank.css',
];

const jsFiles = [
  '01-app.js',
  '06-overrides.js',
  '07-terminology.js',
  '08-layout.js',
  '09-result.js',
  '10-refinements.js',
  '11-probability-rank.js',
];

function findDataStatementEnd(source) {
  const marker = 'const DATA=';
  const start = source.indexOf(marker);
  if (start !== 0) throw new Error('主脚本未以 const DATA= 开始');

  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let index = marker.length; index < source.length; index += 1) {
    const char = source[index];
    if (inString) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === '"') inString = false;
      continue;
    }
    if (char === '"') inString = true;
    else if (char === '{') depth += 1;
    else if (char === '}' && --depth === 0) {
      return source[index + 1] === ';' ? index + 2 : index + 1;
    }
  }
  throw new Error('DATA 对象未正常闭合');
}

await mkdir(legacyDir, { recursive: true });
await mkdir(cssDir, { recursive: true });
await mkdir(jsDir, { recursive: true });
await mkdir(dataDir, { recursive: true });

let monolith;
try {
  monolith = await readFile(snapshotPath, 'utf8');
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
  await copyFile(indexPath, snapshotPath);
  monolith = await readFile(indexPath, 'utf8');
}

const styleMatches = [...monolith.matchAll(/<style([^>]*)>([\s\S]*?)<\/style>/gi)];
const scriptMatches = [...monolith.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/gi)];
if (styleMatches.length !== cssFiles.length || scriptMatches.length !== jsFiles.length) {
  throw new Error(`预期 7 个 style 和 7 个 script，实际为 ${styleMatches.length} 和 ${scriptMatches.length}`);
}

let splitHtml = monolith;
for (let index = 0; index < styleMatches.length; index += 1) {
  const match = styleMatches[index];
  const file = cssFiles[index];
  await writeFile(path.join(cssDir, file), match[2], 'utf8');
  splitHtml = splitHtml.replace(match[0], `<link rel="stylesheet" href="./assets/css/${file}">`);
}

const mainScript = scriptMatches[0][2];
const dataEnd = findDataStatementEnd(mainScript);
await writeFile(path.join(dataDir, 'forecast-data.js'), mainScript.slice(0, dataEnd), 'utf8');
await writeFile(path.join(jsDir, jsFiles[0]), mainScript.slice(dataEnd), 'utf8');
splitHtml = splitHtml.replace(
  scriptMatches[0][0],
  '<script src="./data/forecast-data.js"></script>\n<script src="./assets/js/01-app.js"></script>',
);

for (let index = 1; index < scriptMatches.length; index += 1) {
  const match = scriptMatches[index];
  const file = jsFiles[index];
  await writeFile(path.join(jsDir, file), match[2], 'utf8');
  const id = /\bid="([^"]+)"/.exec(match[1])?.[1];
  splitHtml = splitHtml.replace(
    match[0],
    `<script${id ? ` id="${id}"` : ''} src="./assets/js/${file}"></script>`,
  );
}

await writeFile(indexPath, splitHtml, 'utf8');
console.log('已保留原始快照，并按原顺序拆分 7 个样式、1 个数据文件和 7 个脚本。');
