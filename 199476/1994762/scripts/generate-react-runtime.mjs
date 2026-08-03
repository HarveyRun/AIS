import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const appRoot = path.join(projectRoot, 'ssq');
const sourceDir = path.join(appRoot, 'assets', 'js');
const outputDir = path.join(appRoot, 'src', 'legacy');
const dataDir = path.join(appRoot, 'src', 'data');

const runtimeFiles = [
  '01-app.js',
  '06-overrides.js',
  '07-terminology.js',
  '08-layout.js',
  '09-result.js',
  '10-refinements.js',
  '11-probability-rank.js',
];

function removeImperativeNavigation(source) {
  return source
    .replace(/function gotoPage\(id\)\{[\s\S]*?\n\}\n\$\$\('\.nav button'\)\.forEach\(b=>b\.onclick=\(\)=>gotoPage\(b\.dataset\.page\)\);\n\n/, '')
    .replace(/function profTabs\(\)\{[\s\S]*?\n\}\n(?=function renderProfessional)/, '')
    .replace(/function historyNav\(\)\{[\s\S]*?\n\}\n(?=function renderOverview)/, '')
    .replace('historyNav();renderOverview();', 'renderOverview();')
    .replace('renderDimensions();profTabs();renderProfessional();', 'renderDimensions();renderProfessional();')
    .replace('function renderProfessional(){', "function renderProfessional(){\n  if(!$('#modelWeights'))return;")
    .replace(
      "$('#openAddonsBtn').onclick=()=>{const d=$('#addonMaster');d.open=true;d.scrollIntoView({behavior:'smooth',block:'start'})};",
      "{const el=$('#openAddonsBtn');if(el)el.onclick=()=>{const d=$('#addonMaster');d.open=true;d.scrollIntoView({behavior:'smooth',block:'start'})}};",
    );
}

await mkdir(outputDir, { recursive: true });
await mkdir(dataDir, { recursive: true });

const monolith = await readFile(path.join(appRoot, 'legacy', 'index.monolith.html'), 'utf8');
const body = /<body>([\s\S]*?)<script>const DATA=/i.exec(monolith)?.[1];
if (!body) throw new Error('无法从原页面提取 body 模板');
await writeFile(path.join(outputDir, 'body.html'), body, 'utf8');

const dataSource = await readFile(path.join(appRoot, 'data', 'forecast-data.js'), 'utf8');
const dataJson = dataSource.slice('const DATA='.length, dataSource.endsWith(';') ? -1 : undefined);
JSON.parse(dataJson);
await writeFile(path.join(dataDir, 'forecast-data.json'), `${dataJson}\n`, 'utf8');

const runtimeParts = [];
for (let index = 0; index < runtimeFiles.length; index += 1) {
  const source = await readFile(path.join(sourceDir, runtimeFiles[index]), 'utf8');
  runtimeParts.push(index === 0 ? removeImperativeNavigation(source) : source);
}

const glossaryCleanup = `
(function cleanupCurrentGlossary(){
  const removedFeatures=new Set(['生成六组号码','61方案','62方案','71方案','72方案','81方案','82方案','83方案','93方案']);
  document.querySelectorAll('.feature-card').forEach(card=>{
    const title=card.querySelector('h3'),copy=card.querySelector('p');if(!title)return;
    if(removedFeatures.has(title.textContent)){card.remove();return;}
    if(title.textContent==='复式预测组'&&copy)copy.textContent='展开查看包含更多红球或蓝球的复式号码，以及对应金额和注数。';
    if(title.textContent==='候选组合'&&copy)copy.textContent='除主推号码外，再展示4组备选号码，每组2元、1注。';
  });
  const featureGrid=document.querySelector('.feature-grid');
  if(featureGrid&&![...featureGrid.querySelectorAll('h3')].some(title=>title.textContent==='预测档案')){
    featureGrid.insertAdjacentHTML('beforeend','<article class="feature-card"><h3>预测档案</h3><p>保存每期真实发布的预测、开奖结果、算法版本和号码入选依据。</p></article>');
  }

  const removedTerms=new Set(['PMI','Z值','卡方检验','p值','相关系数','对数损失','Canvas图表']);
  const renamedTerms=new Map([
    ['进入下一期概率','下期号码概率'],
    ['号码分散度（AC值）','号码分散度'],
    ['共现次数','号码同时出现次数'],
    ['时间切分回测','历史检验'],
    ['模型排名','号码排名'],
  ]);
  document.querySelectorAll('.term-card-v7').forEach(card=>{
    const title=card.querySelector('h3');if(!title)return;
    if(removedTerms.has(title.textContent)){card.remove();return;}
    if(renamedTerms.has(title.textContent))title.textContent=renamedTerms.get(title.textContent);
  });
})();
`;

const runtime = `// Generated from the verified legacy runtime. Do not edit directly.\nlet booted = false;\n\nexport function bootLegacyRuntime(DATA) {\n  if (booted) return;\n  booted = true;\n${runtimeParts.join('\n\n')}\n${glossaryCleanup}\n  window.__drawVisibleCharts = drawVisibleCharts;\n}\n`;
await writeFile(path.join(outputDir, 'bootLegacy.js'), runtime, 'utf8');
console.log('已生成 React 模板、数据模块和兼容运行时。');
