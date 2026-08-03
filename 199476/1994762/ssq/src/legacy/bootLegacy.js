// Generated from the verified legacy runtime. Do not edit directly.
let booted = false;

export function bootLegacyRuntime(DATA) {
  if (booted) return;
  booted = true;

const DIM_PLAIN={"期号连续性":["历史期号是否连续","检查有没有缺失、重复或顺序错误。","数据完整，后面的次数和规律才可信。","数据检查"],"来源分层":["区分不同数据来源","把上传数据、官网补充数据、中奖明细分开计算。","避免不同口径混在一起影响结果。","数据检查"],"字段完整率":["每项数据是否齐全","检查号码、日期、销量、奖池等字段缺失情况。","只在字段完整的范围内使用该项数据。","数据检查"],"全历史频率":["全部历史出现次数","统计每个号码长期出现了多少次。","找出长期偏高或偏低的号码。","单个号码"],"多窗口频率":["近期和长期次数对比","同时看近10、20、50、100、200期。","判断一个号码是短期变化还是长期稳定。","单个号码"],"指数加权热度":["越近的数据影响越大","较新的开奖给予更高权重。","更快发现近期变化，又不只追最近一两期。","单个号码"],"遗漏长度":["多久没有出现","计算当前、平均和最长未出现期数。","判断当前间隔是否明显长于自己的历史。","单个号码"],"条件回补率":["相似间隔后是否容易出现","查找历史上同样间隔之后的下一期表现。","把“很久没出”转成可验证的数据。","单个号码"],"1–5期滞后":["前几期是否影响下一期","查看号码在前1至5期出现后，下一期的表现。","寻找重号、隔期号和短周期关系。","前后期关系"],"上期集合转移":["上期整组号码的后续影响","统计上期六个红球出现后，哪些号码更常在下一期出现。","不只看单个上期号码，而是看整组背景。","前后期关系"],"星期分层":["不同开奖日是否不同","分别统计周二、周四、周日。","检查不同开奖日是否存在稳定差别。","时间变化"],"分段稳定性":["规律能否长期保持","按年份和滚动时间段比较。","只采用在多个时间段都能出现的关系。","时间变化"],"滞后自相关":["相邻期是否有关系","检查同一号码前后期是否相互影响。","判断历史结果是不是接近独立。","前后期关系"],"和值":["六个红球的总和","统计历史上各种总和值出现次数。","避免组合落在极少出现的极端范围。","整组号码"],"跨度":["最大号减最小号","统计一组红球覆盖的范围。","避免号码过度集中或过度分散。","整组号码"],"奇偶比":["奇数和偶数各有几个","统计历史奇偶数量组合。","比较候选组合是否接近常见形态。","整组号码"],"大小比":["小号和大号各有几个","01–16算小号，17–33算大号。","避免所有号码集中在一侧。","整组号码"],"三区比":["三个号码区间各有几个","分别统计01–11、12–22、23–33。","检查整组号码在三个区间中的覆盖。","整组号码"],"连号":["连续号码有几组","例如09和10算一组连号。","比较候选中的连续号码数量是否常见。","整组号码"],"重号":["与上期重复几个","统计本期与上期相同的红球数。","把历史上常见的重复数量加入组合判断。","整组号码"],"AC值":["号码是否分散","计算号码之间不同差值的数量。","差值越多，整组号码通常越分散。","整组号码"],"尾数多样性":["个位数是否集中","统计不同尾数和同尾号码对。","避免候选号码的个位数过度集中。","整组号码"],"质数数量":["质数有几个","统计每组号码中质数的数量。","作为较弱的形态参考。","整组号码"],"红球对PMI":["两个号码是否经常一起出现","排除单个号码本身偏热后，再看两者共同出现强度。","寻找稳定的双号码搭配。","号码搭配"],"三元组合提升度":["三个号码是否经常一起出现","比较实际共同出现次数和平均情况下的次数。","寻找稳定的三号码搭配。","号码搭配"],"红蓝同号交叉":["同一数字在红球和蓝球中的关系","查看01–16在红球和蓝球身份之间是否有关联。","检查红蓝球之间是否存在可重复关系。","红蓝关系"],"销量与奖池":["销量和奖池是否与号码有关","检查销量、奖池和号码形态之间的关系。","判断开奖外的业务数据是否能提供信号。","外部数据"],"一等奖注数":["一等奖数量与什么有关","比较一等奖注数、销量和号码形态。","寻找中奖结果中的异常背景。","外部数据"],"省市分布":["哪些省市一等奖更多","统计各省市一等奖注数、涉及期数和网点数。","发现地域长期集中现象。","中奖分布"],"网点跨期重复":["哪些网点多期中奖","统计同一销售网点在不同期次出现的次数。","为异常中奖分布提供核查线索。","中奖分布"],"卡方均匀性":["全部号码是否大致平均","比较实际次数和平均情况下应有的次数。","发现总体分布是否明显偏离。","异常检查"],"极端Z分数":["哪个号码偏离平均最多","计算每个号码偏离平均水平的程度。","快速定位最值得继续分段检查的号码。","异常检查"],"重复完整组合":["完整号码是否曾经重复","检查6红1蓝和仅6红的历史重复。","判断罕见重复事件是否异常集中。","异常检查"],"变点与漂移":["规律从什么时候发生变化","寻找分布突然改变或逐渐移动的时间点。","如果取得设备数据，可把变化对应到设备或流程。","异常检查"],"时间切分":["用过去预测未来","训练数据必须早于验证数据。","防止把未来结果误当成预测能力。","结果验证"],"多模型融合":["多种算法一起判断","把三种算法按回测表现合并。","降低单一算法偶然失效的影响。","结果验证"],"随机基线":["和随便选号码比较","把模型命中结果和平均随机水平对比。","判断模型是否真的提高了结果。","结果验证"],"组合枚举":["逐组比较候选号码","在高分号码池中生成大量组合并逐一打分。","从单个号码概率进入完整组合选择。","结果验证"],"设备与球组":["摇奖机和球组是否造成偏差","需要设备编号、球组编号、称重和维护记录。","把号码变化对应到具体设备或球组。","人为控制线索"],"试机与摇奖顺序":["正式开奖过程是否有异常","需要试机号码、摇出顺序、原始视频和时间记录。","检查试机与正式开奖之间是否存在可重复关系。","人为控制线索"],"销售封盘数据":["截止前是否出现异常投注","需要每注号码、终端、时间和封盘记录。","判断是否有人在结果产生前集中购买最终号码。","人为控制线索"],"权限与审计日志":["谁能查看或修改开奖结果","需要后台访问、审批、签名和发布记录。","检查是否存在提前查看或改写结果的路径。","人为控制线索"]};
const VULNS=[["摇奖设备或球组偏差","设备编号、球组编号、重量尺寸、维护更换、每期号码","按设备和球组分组比较号码频率，做变点和稳定性检验","若偏差能跨期稳定重复，可把设备信息直接加入预测"],["试机与正式摇奖存在固定关系","试机号码、正式摇出顺序、连续原始视频、可信时间","比较试机到正式号码的转移关系，检查视频和事件时间是否连续","若关系能回测，可形成开奖前的新增信号"],["封盘时间不同步","各渠道截止时间、入库时间、拒单时间、可信时钟记录","比较不同渠道的时间偏差和封盘前后订单分布","发现某些渠道是否拥有额外时间窗口"],["开奖结果多来源不一致","官方原始结果、签名、发布时间、页面和接口快照","逐期比较内容哈希、版本号和发布时间差","发现结果链路是否存在提前发布或事后改写迹象"],["公开接口缓存或版本泄漏","公开接口响应头、缓存版本、页面资源时间、公开历史快照","被动比较公开端点的版本和时间差，不绕过认证","若公开端点存在稳定提前量，可作为流程异常证据"],["后台权限过宽或审批链缺口","书面授权提供的角色权限、访问日志、审批记录、签名记录","在隔离环境检查最小权限、双人审批和异常访问","确认是否存在提前知悉或改写结果的技术路径"],["异常投注集中","脱敏每注号码、终端、秒级购买时间、投注方式","做时间聚类、号码集中度和异常收益率比较","区分偶然复式中奖与开奖前异常集中购买"],["网点或关联网络异常","网点、期号、注数、匿名票号和人员关联数据","建立网点—期号—票号关系图，比较重复频率与随机基线","为提前知悉假设提供可继续核验的对象"]];
const LEGAL_PATHS=[["《彩票管理条例》第20、24条","每期销售情况、开奖结果以及发行销售信息","持续保存官方公告，统一期号、销售额、奖池和发布时间","补齐公开时间序列，检查发布时间、销售额和号码之间的关系"],["《彩票管理条例》第21至23条","开奖规则、监督职责、销售数据封存要求、开奖设备管理要求","按条文逐项建立“应有记录清单”，核对已公开内容和实际流程","明确人为控制假设需要哪些过程证据"],["《彩票管理条例实施细则》第32至39条","开奖方式、时间、地点，设备检测、结果签字确认、销售和开奖公告","收集游戏公告、操作规则、监督办法和公开检查材料","把开奖日、地点、设备流程和异常中断加入分层分析"],["《政府信息公开条例》第27条","行政机关已经制作或获取、且依法可以公开的监管信息","向履行彩票监管职能的行政机关提出具体、可检索的信息公开申请","争取获得审批文件、监督制度、公开检查结果等新增数据"],["社会公众监督和书面反映","公开数据不一致、公告延迟、流程异常等可核验事实","按期号、日期、网址、截图哈希和计算过程整理材料后提交监管部门","推动获得正式答复或更多可核对材料"],["电子证据固定","网页、接口结果、视频、时间戳和文件版本","保存原始文件、网页快照、哈希值、抓取时间和多来源对照","让异常线索可复核，避免数据后来变化后无法证明"]];
const LEGAL_SOURCES=[["彩票管理条例","发行销售公开、销售数据封存、开奖设备和开奖结果公告","https://www.mof.gov.cn/zhengwuxinxi/zhengcefabu/200905/t20090513_156600.htm"],["彩票管理条例实施细则","开奖方式、设备检测、结果确认和公告内容","https://tfs.mof.gov.cn/caizhengbuling/201808/t20180831_3004117.htm"],["政府信息公开条例","公民、法人和其他组织可以依法申请获取相关政府信息","https://www.beijing.gov.cn/zhengce/gwywj/201905/t20190522_61986.html"]];

const $=s=>document.querySelector(s), $$=s=>[...document.querySelectorAll(s)];
const pad=n=>String(n).padStart(2,'0');
const pct=(v,d=2)=>(Number(v)*100).toFixed(d)+'%';
const num=(v,d=0)=>v==null?'—':Number(v).toLocaleString('zh-CN',{minimumFractionDigits:d,maximumFractionDigits:d});
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
const money=v=>v==null?'—':(Number(v)>=1e8?(Number(v)/1e8).toFixed(2)+'亿':Number(v)>=1e4?(Number(v)/1e4).toFixed(2)+'万':num(v));
let drawPage=1,outletPage=1;

function ballsHTML(reds,blues,small=false){
  const rc=small?'mini':'ball red',bc=small?'mini blue':'ball blue';
  return reds.map(n=>`<span class="${rc}">${pad(n)}</span>`).join('')+
    '<span class="plus">+</span>'+blues.map(n=>`<span class="${bc}">${pad(n)}</span>`).join('');
}

function initMeta(){
  $('#forecastTitle').textContent=`第 ${DATA.meta.forecastIssue} 期 · ${DATA.meta.forecastDate}`;
  $('#endIssue').textContent=DATA.meta.dataEndIssue;
  $('#nextIssue').textContent=DATA.meta.forecastIssue;
  $('#lastDraw').textContent=DATA.meta.lastDraw.reds.map(pad).join(' ')+' + '+pad(DATA.meta.lastDraw.blue);
  $('#regionRange').textContent=DATA.meta.detailStartIssue+'–'+DATA.meta.detailEndIssue;
}

function schemeDetailHTML(s){
  const adds=s.additions.length?`新增红球：${s.additions.map(pad).join('、')}`:'这是61核心号码，没有新增红球';
  const redRows=s.redDetails.map(r=>`<div class="reason-row"><span class="num-dot">${pad(r.number)}</span><span><b>${esc(r.role)}</b> · 预测概率${pct(r.prob)} · 排名第${r.rank}<br><small class="muted">历史出现${r.history}次，近20期${r.recent20}次，近50期${r.recent50}次，已经${r.gap}期没出</small></span><b>${r.lift>=1?'高于':'低于'}平均${Math.abs((r.lift-1)*100).toFixed(1)}%</b></div>`).join('');
  const blueRows=s.blueDetails.map(r=>`<div class="reason-row"><span class="num-dot blue">${pad(r.number)}</span><span><b>蓝球候选</b> · 预测概率${pct(r.prob)} · 排名第${r.rank}<br><small class="muted">历史出现${r.history}次，近20期${r.recent20}次，已经${r.gap}期没出</small></span><b>${r.lift>=1?'高于':'低于'}平均${Math.abs((r.lift-1)*100).toFixed(1)}%</b></div>`).join('');
  const q=s.subsetQuality;
  return `<div class="simple-score"><div><b>${pct(s.redCoverage,5)}</b><span class="tiny">红球范围覆盖</span></div><div><b>${pct(s.blueCoverage,3)}</b><span class="tiny">蓝球范围覆盖</span></div><div><b>${pct(q.commonRate,1)}</b><span class="tiny">符合常见号码形态</span></div></div><p class="sub">${adds}。这组红球可拆成${q.total}组六红组合；红球总和范围${q.sumMin}–${q.sumMax}，最大最小差范围${q.spanMin}–${q.spanMax}。</p><div class="scheme-detail-grid"><div><h4>红球为什么入选</h4>${redRows}</div><div><h4>蓝球为什么入选</h4>${blueRows}<h4 style="margin-top:14px">最接近历史常见形态的六红组合</h4>${q.bestSubsets.map(x=>`<div class="candidate">${x.map(n=>`<span class="mini">${pad(n)}</span>`).join('')}</div>`).join('')}</div></div>`;
}

function schemeCard(s,primary=false){
  const detailId='scheme-detail-'+s.code;
  const detailButton=primary?'':`<div class="actions" style="margin-top:12px"><button class="btn btn-blue scheme-detail-btn" data-detail="${detailId}">查看${s.code}预测原因</button></div>`;
  return `<div class="${primary?'':'scheme-card'}"><div class="scheme-card-head"><div><span class="scheme-code">${s.code}<span class="tip" tabindex="0" data-tip="${esc(s.label)}，一共覆盖${s.bets}注标准6红1蓝号码。">!</span></span><span class="scheme-name">${esc(s.label)}</span></div></div><div class="ball-row">${ballsHTML(s.reds,s.blues)}</div><div class="scheme-kpis"><div class="kpi"><div class="value">${s.bets}</div><div class="label">覆盖标准注数</div></div><div class="kpi"><div class="value">${pct(s.jointCoverage,5)}</div><div class="label">本方案预测覆盖 <span class="tip" tabindex="0" data-tip="把每个号码的预测概率合并后，估算这组复式号码覆盖完整开奖号码的比例。">!</span></div></div><div class="kpi"><div class="value">${s.relativeTo61.toFixed(1)}×</div><div class="label">相对61覆盖</div></div><div class="kpi"><div class="value">${s.modelLift.toFixed(2)}×</div><div class="label">比平均选号</div></div></div>${detailButton}<div class="scheme-detail" id="${detailId}">${schemeDetailHTML(s)}</div></div>`;
}

function bindSchemeButtons(){
  $$('.scheme-detail-btn').forEach(b=>b.onclick=()=>document.getElementById(b.dataset.detail).classList.toggle('open'));
}
function renderSchemes(animate=false){
  const schemes=DATA.prediction.schemes;
  $('#primaryScheme').innerHTML=schemeCard(schemes[0],true);
  $('#schemeGrid').innerHTML=schemes.slice(1).map(s=>schemeCard(s)).join('');
  bindSchemeButtons();
  if(animate){
    $$('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{
      el.style.opacity='0';el.style.transform='scale(.5)';
      setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35);
    });
  }
}
{const el=$('#generateAllBtn');if(el)el.onclick=()=>renderSchemes(true);}
{const el=$('#primaryDetailBtn');if(el)el.onclick=()=>{const x=$('#scheme-detail-61');if(x){x.classList.add('open');x.scrollIntoView({behavior:'smooth',block:'center'})}};}
{const el=$('#openAddonsBtn');if(el)el.onclick=()=>{const d=$('#addonMaster');d.open=true;d.scrollIntoView({behavior:'smooth',block:'start'})}};

function stateBadge(s){return `<span class="badge ${s==='偏高'?'hot':s==='偏低'?'cold':''}">${esc(s)}</span>`}
function renderProbability(){
  const r=DATA.probabilities.red,b=DATA.probabilities.blue;
  const rmax=Math.max(...r.map(x=>x.modelProb)),rmin=Math.min(...r.map(x=>x.modelProb));
  const bmax=Math.max(...b.map(x=>x.modelProb)),bmin=Math.min(...b.map(x=>x.modelProb));
  $('#redHeat').innerHTML=r.map(x=>`<div class="prob-ball ${x.status==='偏高'?'hot':x.status==='偏低'?'cold':''}" style="opacity:${.72+.28*(x.modelProb-rmin)/(rmax-rmin)}"><span class="n">${pad(x.number)}</span><span class="p">${pct(x.modelProb)}</span></div>`).join('');
  $('#blueHeat').innerHTML=b.map(x=>`<div class="prob-ball ${x.status==='偏高'?'hot':x.status==='偏低'?'cold':''}" style="opacity:${.72+.28*(x.modelProb-bmin)/(bmax-bmin)}"><span class="n">${pad(x.number)}</span><span class="p">${pct(x.modelProb)}</span></div>`).join('');
  renderProbTables();
}
function renderProbTables(){
  const rq=$('#redProbSearch')?.value?.trim?.()||'',bq=$('#blueProbSearch')?.value?.trim?.()||'';
  const diff=r=>(r.lift>=1?'+':'-')+Math.abs((r.lift-1)*100).toFixed(1)+'%';
  const redProbBody=document.querySelector('#redProbTable tbody');if(redProbBody)redProbBody.innerHTML=DATA.probabilities.red.filter(r=>!rq||pad(r.number).includes(rq)).sort((a,b)=>a.rank-b.rank).map(r=>`<tr><td><span class="num-dot">${pad(r.number)}</span></td><td>${pct(r.modelProb)}<div class="bar"><i style="width:${r.modelProb/.21*100}%"></i></div></td><td>第${r.rank}</td><td>${diff(r)}</td><td>${r.redCount}</td><td>${r.recent20}</td><td>${r.recent50}</td><td>${r.gap}期</td><td>${stateBadge(r.status)}</td></tr>`).join('');
  const blueProbBody=document.querySelector('#blueProbTable tbody');if(blueProbBody)blueProbBody.innerHTML=DATA.probabilities.blue.filter(r=>!bq||pad(r.number).includes(bq)).sort((a,b)=>a.rank-b.rank).map(r=>`<tr><td><span class="num-dot blue">${pad(r.number)}</span></td><td>${pct(r.modelProb)}<div class="bar blue"><i style="width:${r.modelProb/.08*100}%"></i></div></td><td>第${r.rank}</td><td>${diff(r)}</td><td>${r.count}</td><td>${r.recent20}</td><td>${r.recent50}</td><td>${r.gap}期</td><td>${stateBadge(r.status)}</td></tr>`).join('');
}

function renderDimensions(){
  const order=['数据检查','单个号码','前后期关系','时间变化','整组号码','号码搭配','红蓝关系','外部数据','中奖分布','异常检查','结果验证','人为控制线索'];
  const grouped={};
  DATA.dimensions.forEach(x=>{const p=DIM_PLAIN[x.name]||[x.name,x.description,x.use,x.group];(grouped[p[3]]??=[]).push([x,p])});
  $('#dimensionGridV3').innerHTML=order.filter(g=>grouped[g]).map(g=>`<section class="plain-dim-group"><h3>${g}</h3><div class="plain-dim-cards">${grouped[g].map(([x,p])=>`<article class="plain-dim-card"><h4>${esc(p[0])} <span class="tip" tabindex="0" data-tip="专业名称：${esc(x.name)}。${esc(x.description)}">!</span></h4><p><b>分析：</b>${esc(p[1])}</p><p><b>作用：</b>${esc(p[2])}</p></article>`).join('')}</div></section>`).join('');
}

function renderProfessional(){
  if(!$('#modelWeights'))return;
  const w=DATA.prediction.modelWeights,a=DATA.audit;
  $('#modelWeights').innerHTML=`<div class="metric"><span>红球逻辑回归</span><b>${pct(w.red.logistic,0)}</b></div><div class="metric"><span>红球梯度提升</span><b>${pct(w.red.gradientBoosting,0)}</b></div><div class="metric"><span>红球极端随机树</span><b>${pct(w.red.extraTrees,0)}</b></div><div class="metric"><span>蓝球逻辑回归</span><b>${pct(w.blue.logistic,0)}</b></div><div class="metric"><span>蓝球梯度提升</span><b>${pct(w.blue.gradientBoosting,0)}</b></div>`;
  $('#schemeMath').innerHTML=DATA.prediction.schemes.map(s=>`<div class="metric"><span>${s.code} · ${esc(s.label)}</span><b>C(${s.redCount},6)×${s.blueCount}=${s.bets}</b></div>`).join('');
  $('#modelCompare').innerHTML=`<div class="grid2"><div><h3>红球模型</h3>${DATA.backtest.modelCompare.red.map(x=>`<div class="metric"><span>${esc(x.model)}</span><b>前6名命中 ${x.top6.toFixed(3)} · 对数损失 ${x.logloss.toFixed(4)}</b></div>`).join('')}</div><div><h3>蓝球模型</h3>${DATA.backtest.modelCompare.blue.map(x=>`<div class="metric"><span>${esc(x.model)}</span><b>第1名命中 ${pct(x.top1)} · 对数损失 ${x.logloss.toFixed(4)}</b></div>`).join('')}</div></div>`;
  $('#backtestCards').innerHTML=[['红球前6名',DATA.backtest.redTop6.toFixed(3),'平均水平'+DATA.backtest.redRandomTop6.toFixed(3)],['红球前10名',DATA.backtest.redTop10.toFixed(3),'平均水平'+DATA.backtest.redRandomTop10.toFixed(3)],['蓝球第1名',pct(DATA.backtest.blueTop1),'平均水平'+pct(DATA.backtest.blueRandomTop1)],['蓝球前3名',pct(DATA.backtest.blueTop3),'平均水平'+pct(DATA.backtest.blueRandomTop3)],['测试范围',DATA.backtest.testDraws+'期',DATA.backtest.startIssue+'–'+DATA.backtest.endIssue]].map(x=>`<div class="stat-card"><div class="v">${x[1]}</div><div class="n">${x[0]} · ${x[2]}</div></div>`).join('');
  $('#auditCards').innerHTML=[['红球整体平均检验',a.redP.toFixed(4),'p值'],['蓝球整体平均检验',a.blueP.toFixed(4),'p值'],['红球相邻期关系',a.redAutoAvg.toFixed(4),'平均相关'],['蓝球相邻期关系',a.blueAutoAvg.toFixed(4),'平均相关'],['省市集中程度',a.provinceHHI.toFixed(4),'HHI']].map(x=>`<div class="stat-card"><div class="v">${x[1]}</div><div class="n">${x[0]} · ${x[2]}</div></div>`).join('');
  $('#correlationTable tbody').innerHTML=a.correlations.map(x=>`<tr><td>${esc(x.x)}</td><td>${esc(x.y)}</td><td>${x.r.toFixed(4)}</td><td>${Number(x.p).toExponential(2)}</td><td>${x.n}</td></tr>`).join('');
  $('#redDeviationList').innerHTML=[...a.redDeviations].sort((x,y)=>Math.abs(y.z)-Math.abs(x.z)).slice(0,12).map(x=>`<div class="metric"><span>红球${pad(x.number)} · ${x.count}次</span><b class="${Math.abs(x.z)>=2?'warn':''}">Z=${x.z.toFixed(3)}</b></div>`).join('');
  $('#duplicateSummary').innerHTML=`<div class="metric"><span>完整6红+1蓝重复</span><b>${a.duplicateFull.length}</b></div><div class="metric"><span>仅6个红球重复</span><b>${a.duplicateRed.length}</b></div>`+a.duplicateRed.map(x=>`<div class="metric"><span>${x.reds.map(pad).join(' ')}</span><b>${x.count}次</b></div>`).join('');
  $('#hypothesisGrid').innerHTML=DATA.professionalModels.map(x=>`<div class="hypothesis"><h3>${esc(x.name)}<span>${esc(x.term)}</span></h3><dl><dt>研究问题</dt><dd>${esc(x.question)}</dd><dt>需要数据</dt><dd>${esc(x.observable)}</dd><dt>检查方法</dt><dd>${esc(x.method)}</dd><dt>当前数据结果</dt><dd>${esc(x.current)}</dd></dl></div>`).join('');
  $('#evidenceCoverage').innerHTML='<h3>现有数据覆盖</h3>'+a.evidenceCoverage.map(x=>`<div class="evidence-row"><span>${esc(x.name)}</span><div class="progress"><i style="width:${x.coverage}%"></i></div><b>${x.coverage}%</b></div>`).join('');
  $('#controlDataNeeded').innerHTML=DATA.dataNeeded.slice(0,5).map(x=>`<div class="metric"><span><b>${x.priority}</b> ${esc(x.data)}</span></div>`).join('');
  $('#securityTableV3 tbody').innerHTML=VULNS.map(x=>`<tr><td style="white-space:normal;text-align:left"><b>${esc(x[0])}</b></td><td style="white-space:normal;text-align:left">${esc(x[1])}</td><td style="white-space:normal;text-align:left">${esc(x[2])}</td><td style="white-space:normal;text-align:left">${esc(x[3])}</td></tr>`).join('');
  $('#legalTableV3 tbody').innerHTML=LEGAL_PATHS.map(x=>`<tr><td style="white-space:normal;text-align:left"><b>${esc(x[0])}</b></td><td style="white-space:normal;text-align:left">${esc(x[1])}</td><td style="white-space:normal;text-align:left">${esc(x[2])}</td><td style="white-space:normal;text-align:left">${esc(x[3])}</td></tr>`).join('');
  $('#legalSources').innerHTML=LEGAL_SOURCES.map(x=>`<div class="source-card"><h3>${esc(x[0])}</h3><p class="muted">${esc(x[1])}</p><a href="${esc(x[2])}" target="_blank" rel="noopener">查看官方原文</a></div>`).join('');
}

function renderOverview(){
  $('#overviewKpis').innerHTML=DATA.stats.historySummary.map(x=>`<div class="panel"><div class="eyebrow">${esc(x.name)}</div><div style="font-size:24px;font-weight:1000;margin:7px 0">${esc(x.value)}</div><div class="muted">${esc(x.note)}</div></div>`).join('');
}
function renderNumberTable(){
  const q=$('#numberSearch').value.trim(),sort=$('#numberSort').value;
  let rows=[...DATA.stats.redNumbers].filter(x=>!q||pad(x.number).includes(q));
  if(sort==='number')rows.sort((a,b)=>a.number-b.number);else rows.sort((a,b)=>(b[sort]??-1)-(a[sort]??-1));
  $('#numberTable tbody').innerHTML=rows.map(r=>`<tr><td><span class="num-dot">${pad(r.number)}</span></td><td>${r.redCount}</td><td>${pct(r.redRate)}</td><td>${r.blueCount||'—'}</td><td>${r.number<=16?pct(r.blueRate):'—'}</td><td>${r.recent10}/${r.recent20}/${r.recent50}/${r.recent100}</td><td>${r.gap}</td><td>${r.avgGap?.toFixed(1)??'—'} / ${r.maxGap??'—'}</td><td>${r.Tue}/${r.Thu}/${r.Sun}</td><td>${r.lastIssue}<br><span class="tiny">${r.lastDate}</span></td><td>${pct(r.modelProb)} · 第${r.rank}</td></tr>`).join('');
}
function renderDrawTable(){
  const q=$('#drawSearch').value.trim().toLowerCase(),year=$('#drawYear').value,ps=Number($('#drawPageSize').value);
  let rows=DATA.stats.draws.filter(d=>(!year||d.date.startsWith(year))&&(!q||[d.issue,d.date,d.reds.join(' '),pad(d.blue)].join(' ').toLowerCase().includes(q))).slice().reverse();
  const pages=Math.max(1,Math.ceil(rows.length/ps));drawPage=Math.min(drawPage,pages);const start=(drawPage-1)*ps;
  $('#drawTable tbody').innerHTML=rows.slice(start,start+ps).map(d=>`<tr><td>${d.issue}</td><td>${d.date}</td><td>${d.weekday}</td><td>${d.reds.map(n=>`<span class="mini">${pad(n)}</span>`).join(' ')}</td><td><span class="mini blue">${pad(d.blue)}</span></td><td>${d.sum}</td><td>${d.span}</td><td>${d.odd}:${d.even}</td><td>${d.small}:${d.big}</td><td>${d.zones.join(':')}</td><td>${d.consecutive}</td><td>${d.repeat}</td><td>${d.ac}</td><td>${d.firstCount??'—'}</td><td>${money(d.sales)}</td><td>${money(d.pool)}</td><td>${d.source==='upload'?'上传数据':'官网补充'}</td></tr>`).join('');
  $('#drawPager').innerHTML=`<span class="tiny">共${num(rows.length)}期 · ${drawPage}/${pages}页</span><button id="drawPrev">上一页</button><button id="drawNext">下一页</button>`;
  $('#drawPrev').onclick=()=>{if(drawPage>1){drawPage--;renderDrawTable()}};
  $('#drawNext').onclick=()=>{if(drawPage<pages){drawPage++;renderDrawTable()}};
}
function renderTimeTables(){
  $('#yearTable tbody').innerHTML=DATA.stats.years.map(x=>`<tr><td>${x.year}</td><td>${x.draws}</td><td>${x.avgSum.toFixed(2)}</td><td>${x.avgSpan.toFixed(2)}</td><td>${x.avgOdd.toFixed(2)}</td><td>${x.avgBig.toFixed(2)}</td><td>${x.avgConsecutive.toFixed(2)}</td><td>${x.avgRepeat.toFixed(2)}</td><td>${money(x.salesTotal)}</td><td>${money(x.avgPool)}</td><td>${num(x.firstWins)}</td></tr>`).join('');
  $('#weekdayTable tbody').innerHTML=DATA.stats.weekdays.map(x=>`<tr><td>${x.weekday}</td><td>${x.draws}</td><td>${x.avgSum.toFixed(2)}</td><td>${x.avgSpan.toFixed(2)}</td><td>${x.avgOdd.toFixed(2)}</td><td>${x.avgBlue.toFixed(2)}</td></tr>`).join('');
}
function renderRegions(){
  $('#provinceTable tbody').innerHTML=DATA.stats.provinces.map((x,i)=>`<tr><td>第${i+1}</td><td>${esc(x.province)}</td><td>${x.wins}</td><td>${pct(x.share)}</td><td>${x.issues}</td><td>${x.records}</td><td>${x.uniqueOutlets}</td><td>${money(x.avgTicketAmount)}</td><td>${money(x.maxTicketAmount)}</td><td>${x.firstIssue}</td><td>${x.lastIssue}</td></tr>`).join('');
  renderOutletTable();
}
function renderOutletTable(){
  const q=$('#outletSearch').value.trim().toLowerCase(),ps=50;
  let rows=DATA.stats.outlets.filter(x=>!q||[x.outlet,x.address,x.provinces].join(' ').toLowerCase().includes(q));
  const pages=Math.max(1,Math.ceil(rows.length/ps));outletPage=Math.min(outletPage,pages);const start=(outletPage-1)*ps;
  $('#outletTable tbody').innerHTML=rows.slice(start,start+ps).map(x=>`<tr><td>${x.outlet}</td><td>${x.issues}</td><td>${x.wins}</td><td>${esc(x.provinces)}</td><td style="white-space:normal;text-align:left;min-width:280px">${esc(x.address)}</td><td>${money(x.avgTicketAmount)}</td><td>${money(x.maxTicketAmount)}</td></tr>`).join('');
  $('#outletPager').innerHTML=`<span class="tiny">共${num(rows.length)}个网点 · ${outletPage}/${pages}页</span><button id="outletPrev">上一页</button><button id="outletNext">下一页</button>`;
  $('#outletPrev').onclick=()=>{if(outletPage>1){outletPage--;renderOutletTable()}};
  $('#outletNext').onclick=()=>{if(outletPage<pages){outletPage++;renderOutletTable()}};
}
function renderNetwork(){
  $('#pairTable tbody').innerHTML=DATA.stats.pairs.slice(0,50).map((x,i)=>`<tr><td>第${i+1}</td><td>${x.pair}</td><td>${x.count}</td><td>${x.lift.toFixed(3)}倍</td><td>${x.pmi.toFixed(3)}</td></tr>`).join('');
  $('#tripleTable tbody').innerHTML=DATA.stats.triples.slice(0,50).map((x,i)=>`<tr><td>第${i+1}</td><td>${x.triple}</td><td>${x.count}</td><td>${x.lift.toFixed(3)}倍</td></tr>`).join('');
}
function csvEscape(v){if(Array.isArray(v))v=v.join(' ');if(v&&typeof v==='object')v=JSON.stringify(v);v=String(v??'');return /[",\n]/.test(v)?'"'+v.replace(/"/g,'""')+'"':v}
function downloadCSV(name,rows){if(!rows.length)return;const keys=Object.keys(rows[0]),csv='\ufeff'+[keys.join(','),...rows.map(r=>keys.map(k=>csvEscape(r[k])).join(','))].join('\n');const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000)}
function bindHistoryEvents(){
  $('#numberSearch').oninput=renderNumberTable;$('#numberSort').onchange=renderNumberTable;
  $('#drawSearch').oninput=()=>{drawPage=1;renderDrawTable()};$('#drawYear').onchange=()=>{drawPage=1;renderDrawTable()};$('#drawPageSize').onchange=()=>{drawPage=1;renderDrawTable()};
  $('#outletSearch').oninput=()=>{outletPage=1;renderOutletTable()};
  $$('[data-export]').forEach(b=>b.onclick=()=>{const map={numbers:['number_statistics.csv',DATA.stats.redNumbers],draws:['draw_history.csv',DATA.stats.draws],years:['year_statistics.csv',DATA.stats.years],provinces:['province_statistics.csv',DATA.stats.provinces],outlets:['outlet_statistics.csv',DATA.stats.outlets]};downloadCSV(...map[b.dataset.export])});
}

function setupCanvas(c){const r=c.getBoundingClientRect(),dpr=window.devicePixelRatio||1;c.width=Math.max(300,r.width*dpr);c.height=Math.max(220,r.height*dpr);const x=c.getContext('2d');x.setTransform(dpr,0,0,dpr,0,0);return{x,w:r.width,h:r.height}}
function axes(x,w,h,l=48,b=38){const a={l,t:14,r:w-12,b:h-b};x.strokeStyle='#344968';x.lineWidth=1;x.beginPath();x.moveTo(a.l,a.t);x.lineTo(a.l,a.b);x.lineTo(a.r,a.b);x.stroke();return a}
function barChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null)return;const{x,w,h}=setupCanvas(c),a=axes(x,w,h,opt.left||48,opt.bottom||38),min=opt.min??0,max=opt.max??(Math.max(...vals)*1.12||1),bw=(a.r-a.l)/vals.length,gap=Math.min(4,bw*.18);x.font='10px Microsoft YaHei';x.textAlign='center';vals.forEach((v,i)=>{const hh=(v-min)/(max-min||1)*(a.b-a.t),xx=a.l+i*bw+gap/2,yy=a.b-hh,g=x.createLinearGradient(0,yy,0,a.b);g.addColorStop(0,opt.c2||'#ff6d7f');g.addColorStop(1,opt.c1||'#d51f45');x.fillStyle=g;x.fillRect(xx,yy,Math.max(2,bw-gap),hh);if(vals.length<=18||i%Math.ceil(vals.length/16)===0){x.fillStyle='#8fa1bf';x.fillText(labels[i],xx+(bw-gap)/2,a.b+15)}});if(opt.line!=null){const yy=a.b-(opt.line-min)/(max-min||1)*(a.b-a.t);x.strokeStyle='#ffc85a';x.setLineDash([5,4]);x.beginPath();x.moveTo(a.l,yy);x.lineTo(a.r,yy);x.stroke();x.setLineDash([])}}
function signedBarChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null)return;const{x,w,h}=setupCanvas(c),a=axes(x,w,h,opt.left||48,opt.bottom||38),min=opt.min??Math.min(...vals,-1),max=opt.max??Math.max(...vals,1),zero=a.b-(0-min)/(max-min||1)*(a.b-a.t),bw=(a.r-a.l)/vals.length,gap=Math.min(4,bw*.18);x.strokeStyle='#ffc85a';x.setLineDash([5,4]);x.beginPath();x.moveTo(a.l,zero);x.lineTo(a.r,zero);x.stroke();x.setLineDash([]);vals.forEach((v,i)=>{const yy=a.b-(v-min)/(max-min||1)*(a.b-a.t),xx=a.l+i*bw+gap/2;x.fillStyle=v>=0?'#ff6678':'#5c9cff';x.fillRect(xx,Math.min(yy,zero),Math.max(2,bw-gap),Math.abs(zero-yy))})}
function lineChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null)return;const{x,w,h}=setupCanvas(c),a=axes(x,w,h,52,38),min=opt.min??Math.min(...vals)*.95,max=opt.max??Math.max(...vals)*1.05;x.strokeStyle=opt.color||'#56a3ff';x.lineWidth=2;x.beginPath();vals.forEach((v,i)=>{const xx=a.l+i/(vals.length-1||1)*(a.r-a.l),yy=a.b-(v-min)/(max-min||1)*(a.b-a.t);i?x.lineTo(xx,yy):x.moveTo(xx,yy)});x.stroke();if(opt.line!=null){const yy=a.b-(opt.line-min)/(max-min||1)*(a.b-a.t);x.strokeStyle='#ffc85a';x.setLineDash([5,4]);x.beginPath();x.moveTo(a.l,yy);x.lineTo(a.r,yy);x.stroke();x.setLineDash([])}}
function drawVisibleCharts(){
  barChart('redProbChart',DATA.probabilities.red.map(x=>pad(x.number)),DATA.probabilities.red.map(x=>x.modelProb*100),{min:16,max:21,line:DATA.meta.redBaseline*100});
  barChart('blueProbChart',DATA.probabilities.blue.map(x=>pad(x.number)),DATA.probabilities.blue.map(x=>x.modelProb*100),{min:5,max:8,line:DATA.meta.blueBaseline*100,c1:'#1765d8',c2:'#6cb4ff'});
  lineChart('redBacktestChart',DATA.backtest.redIssues,DATA.backtest.redRolling,{min:.7,max:1.7,line:DATA.backtest.redRandomTop6});
  lineChart('blueBacktestChart',DATA.backtest.blueIssues,DATA.backtest.blueRolling,{min:0,max:.22,line:DATA.backtest.blueRandomTop1,color:'#40d39c'});
  signedBarChart('redZChart',DATA.audit.redDeviations.map(x=>pad(x.number)),DATA.audit.redDeviations.map(x=>x.z),{min:-3.5,max:3.5});
  barChart('redCountChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.redCount),{line:DATA.meta.drawCount*6/33});
  barChart('blueCountChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.count),{line:DATA.meta.drawCount/16,c1:'#1765d8',c2:'#6cb4ff'});
  barChart('sumChart',DATA.stats.structures.sum.map(x=>x.value),DATA.stats.structures.sum.map(x=>x.count),{c1:'#28518a',c2:'#5bb7ff'});
  barChart('provinceChart',DATA.stats.provinces.slice(0,15).map(x=>x.province),DATA.stats.provinces.slice(0,15).map(x=>x.wins),{left:60,bottom:50,c1:'#18765d',c2:'#46d6a6'});
  const st=DATA.stats.structures;
  barChart('oddChart',st.odd.map(x=>x.value),st.odd.map(x=>x.count),{c1:'#6d2d7e',c2:'#d480ff'});
  barChart('spanChart',st.span.map(x=>x.value),st.span.map(x=>x.count),{c1:'#28518a',c2:'#5bb7ff'});
  barChart('consecutiveChart',st.consecutive.map(x=>x.value),st.consecutive.map(x=>x.count),{c1:'#8a5428',c2:'#ffc16b'});
  barChart('repeatChart',st.repeat.map(x=>x.value),st.repeat.map(x=>x.count),{c1:'#18765d',c2:'#46d6a6'});
  barChart('acChart',st.ac.map(x=>x.value),st.ac.map(x=>x.count),{c1:'#5a3cad',c2:'#a58bff'});
  barChart('tailChart',st.tailUnique.map(x=>x.value),st.tailUnique.map(x=>x.count),{c1:'#166d7e',c2:'#45d6e8'});
  lineChart('yearSumChart',DATA.stats.years.map(x=>String(x.year)),DATA.stats.years.map(x=>x.avgSum),{min:92,max:107,line:99});
  lineChart('yearSpanChart',DATA.stats.years.map(x=>String(x.year)),DATA.stats.years.map(x=>x.avgSpan),{min:22,max:27,color:'#9776ff'});
}
function initHistory(){
  const years=[...new Set(DATA.stats.draws.map(x=>x.date.slice(0,4)))].sort().reverse();
  $('#drawYear').innerHTML='<option value="">全部年份</option>'+years.map(y=>`<option>${y}</option>`).join('');
  renderOverview();renderNumberTable();renderDrawTable();renderTimeTables();renderRegions();renderNetwork();bindHistoryEvents();
}

/* V5 overrides */
function schemeDetailHTML(s){
  const redRows=s.redDetails.map(r=>`<div class="reason-row"><span class="num-dot">${pad(r.number)}</span><span><b>${esc(r.role)}</b><br><small class="muted">进入下一期概率 ${pct(r.prob)}，33个红球中第${r.rank}名；近20期出现${r.recent20}次；已经${r.gap}期没出。</small></span></div>`).join('');
  const blueRows=s.blueDetails.map(r=>`<div class="reason-row"><span class="num-dot blue">${pad(r.number)}</span><span><b>蓝球候选</b><br><small class="muted">成为下一期蓝球的概率 ${pct(r.prob)}，16个蓝球中第${r.rank}名；近20期出现${r.recent20}次；已经${r.gap}期没出。</small></span></div>`).join('');
  return `<div class="scheme-detail-grid"><div><h4>红球预测原因</h4>${redRows}</div><div><h4>蓝球预测原因</h4>${blueRows}</div></div>`;
}
function schemeCard(s,primary=false){
  const detailId='scheme-detail-'+s.code;
  const detailButton=primary?'':`<div class="actions" style="margin-top:12px"><button class="btn btn-blue scheme-detail-btn" data-detail="${detailId}">查看${s.code}预测原因</button></div>`;
  return `<div class="${primary?'':'scheme-card'}"><div class="scheme-card-head"><div><span class="scheme-code">${s.code}</span><span class="scheme-name">${esc(s.label)}</span></div></div><div class="ball-row">${ballsHTML(s.reds,s.blues)}</div><div class="scheme-kpis"><div class="kpi"><div class="value">${s.bets}</div><div class="label">注</div></div></div>${detailButton}<div class="scheme-detail" id="${detailId}">${schemeDetailHTML(s)}</div></div>`;
}
function renderProbability(){
  const r=DATA.probabilities.red,b=DATA.probabilities.blue;
  $('#redHeat').innerHTML=[...r].sort((a,b)=>a.number-b.number).map(x=>`<div class="prob-ball"><span class="n">${pad(x.number)}</span><span class="p">${pct(x.modelProb)}</span></div>`).join('');
  $('#blueHeat').innerHTML=[...b].sort((a,b)=>a.number-b.number).map(x=>`<div class="prob-ball"><span class="n">${pad(x.number)}</span><span class="p">${pct(x.modelProb)}</span></div>`).join('');
}
function renderDimensions(){}
function renderOverview(){}

function canvasBox(c){const r=c.getBoundingClientRect(),dpr=window.devicePixelRatio||1;c.width=Math.max(320,Math.round(r.width*dpr));c.height=Math.max(240,Math.round(r.height*dpr));const ctx=c.getContext('2d');ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,r.width,r.height);return{ctx,w:r.width,h:r.height}}
function niceRange(min,max,ticks=5){if(!isFinite(min)||!isFinite(max)){return{min:0,max:1,step:.2}};if(min===max){min-=1;max+=1}const raw=(max-min)/ticks,pow=Math.pow(10,Math.floor(Math.log10(Math.abs(raw)||1))),n=raw/pow,step=(n<=1?1:n<=2?2:n<=5?5:10)*pow;return{min:Math.floor(min/step)*step,max:Math.ceil(max/step)*step,step}}
function chartBase(ctx,w,h,opt,min,max){const m={l:opt.left||68,r:18,t:18,b:opt.bottom||62};const a={l:m.l,r:w-m.r,t:m.t,b:h-m.b};ctx.font='11px Microsoft YaHei';ctx.fillStyle='#8fa1bf';ctx.strokeStyle='#263b58';ctx.lineWidth=1;const nr=niceRange(min,max,opt.yTicks||5);for(let v=nr.min;v<=nr.max+nr.step*.2;v+=nr.step){const y=a.b-(v-nr.min)/(nr.max-nr.min)*(a.b-a.t);ctx.strokeStyle='#21344f';ctx.beginPath();ctx.moveTo(a.l,y);ctx.lineTo(a.r,y);ctx.stroke();ctx.fillStyle='#8fa1bf';ctx.textAlign='right';ctx.textBaseline='middle';const label=Math.abs(v)>=1000000?(v/1000000).toFixed(1)+'M':Math.abs(v)>=1000?(v/1000).toFixed(1)+'K':Math.abs(v)<1&&v!==0?v.toFixed(2):Number(v.toFixed(2));ctx.fillText(label,a.l-8,y)}ctx.strokeStyle='#55708f';ctx.beginPath();ctx.moveTo(a.l,a.t);ctx.lineTo(a.l,a.b);ctx.lineTo(a.r,a.b);ctx.stroke();ctx.save();ctx.translate(16,(a.t+a.b)/2);ctx.rotate(-Math.PI/2);ctx.fillStyle='#b7c7dd';ctx.textAlign='center';ctx.fillText(opt.yLabel||'数值',0,0);ctx.restore();ctx.fillStyle='#b7c7dd';ctx.textAlign='center';ctx.fillText(opt.xLabel||'横轴', (a.l+a.r)/2,h-10);return{...a,min:nr.min,max:nr.max}}
function xLabels(ctx,a,labels,opt={}){const n=labels.length,step=opt.labelStep||Math.max(1,Math.ceil(n/(opt.maxLabels||16))),rot=opt.rotate||0;ctx.font='10px Microsoft YaHei';ctx.fillStyle='#8fa1bf';for(let i=0;i<n;i+=step){const x=a.l+(n===1?.5:i/(n-1))*(a.r-a.l);ctx.save();ctx.translate(x,a.b+10);ctx.rotate(rot);ctx.textAlign=rot?'right':'center';ctx.textBaseline='top';ctx.fillText(String(labels[i]),0,0);ctx.restore()}}
function barChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!vals.length)return;const{ctx,w,h}=canvasBox(c),min=opt.min??0,max=opt.max??Math.max(...vals)*1.08,a=chartBase(ctx,w,h,opt,min,max),bw=(a.r-a.l)/vals.length,gap=Math.min(5,bw*.22);vals.forEach((v,i)=>{const y=a.b-(v-a.min)/(a.max-a.min)*(a.b-a.t),x=a.l+i*bw+gap/2,g=ctx.createLinearGradient(0,y,0,a.b);g.addColorStop(0,opt.c2||'#ff7182');g.addColorStop(1,opt.c1||'#d51f45');ctx.fillStyle=g;ctx.fillRect(x,y,Math.max(1,bw-gap),a.b-y)});if(opt.line!=null){const y=a.b-(opt.line-a.min)/(a.max-a.min)*(a.b-a.t);ctx.strokeStyle='#ffc85a';ctx.setLineDash([6,4]);ctx.beginPath();ctx.moveTo(a.l,y);ctx.lineTo(a.r,y);ctx.stroke();ctx.setLineDash([])}xLabels(ctx,a,labels,{maxLabels:opt.maxLabels||18,rotate:opt.rotate||0,labelStep:opt.labelStep})}
function lineChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!vals.length)return;const{ctx,w,h}=canvasBox(c),min=opt.min??Math.min(...vals),max=opt.max??Math.max(...vals),a=chartBase(ctx,w,h,opt,min,max);ctx.strokeStyle=opt.color||'#56a3ff';ctx.lineWidth=2;ctx.beginPath();vals.forEach((v,i)=>{const x=a.l+i/(vals.length-1||1)*(a.r-a.l),y=a.b-(v-a.min)/(a.max-a.min)*(a.b-a.t);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke();if(opt.line!=null){const y=a.b-(opt.line-a.min)/(a.max-a.min)*(a.b-a.t);ctx.strokeStyle='#ffc85a';ctx.setLineDash([6,4]);ctx.beginPath();ctx.moveTo(a.l,y);ctx.lineTo(a.r,y);ctx.stroke();ctx.setLineDash([])}xLabels(ctx,a,labels,{maxLabels:opt.maxLabels||10,rotate:opt.rotate||0})}
function multiLineChart(id,labels,series,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!series.length)return;const all=series.flatMap(s=>s.values),{ctx,w,h}=canvasBox(c),a=chartBase(ctx,w,h,opt,opt.min??Math.min(...all),opt.max??Math.max(...all)),colors=['#56a3ff','#ff6678','#40d39c','#ffc85a','#a58bff','#35d3d0'];series.forEach((s,si)=>{ctx.strokeStyle=colors[si%colors.length];ctx.lineWidth=2;ctx.beginPath();s.values.forEach((v,i)=>{const x=a.l+i/(labels.length-1||1)*(a.r-a.l),y=a.b-(v-a.min)/(a.max-a.min)*(a.b-a.t);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke()});xLabels(ctx,a,labels,{maxLabels:opt.maxLabels||10});let lx=a.l;series.forEach((s,si)=>{ctx.fillStyle=colors[si%colors.length];ctx.fillRect(lx,a.t+2,10,3);ctx.fillStyle='#b9c7da';ctx.textAlign='left';ctx.fillText(s.name,lx+14,a.t+5);lx+=ctx.measureText(s.name).width+38})}
function groupedBarChart(id,labels,series,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!series.length)return;const all=series.flatMap(s=>s.values),{ctx,w,h}=canvasBox(c),a=chartBase(ctx,w,h,opt,opt.min??0,opt.max??Math.max(...all)*1.08),colors=['#ff6678','#56a3ff','#40d39c','#ffc85a'],groupW=(a.r-a.l)/labels.length,barW=groupW*.78/series.length;series.forEach((s,si)=>s.values.forEach((v,i)=>{const x=a.l+i*groupW+groupW*.11+si*barW,y=a.b-(v-a.min)/(a.max-a.min)*(a.b-a.t);ctx.fillStyle=colors[si%colors.length];ctx.fillRect(x,y,Math.max(1,barW-1),a.b-y)}));xLabels(ctx,a,labels,{maxLabels:opt.maxLabels||16,rotate:opt.rotate||0});let lx=a.l;series.forEach((s,si)=>{ctx.fillStyle=colors[si%colors.length];ctx.fillRect(lx,a.t+2,10,8);ctx.fillStyle='#b9c7da';ctx.textAlign='left';ctx.fillText(s.name,lx+14,a.t+7);lx+=ctx.measureText(s.name).width+38})}
function hBarChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!vals.length)return;const{ctx,w,h}=canvasBox(c),left=opt.left||105,right=25,top=18,bottom=48,a={l:left,r:w-right,t:top,b:h-bottom},max=opt.max??Math.max(...vals)*1.06,bh=(a.b-a.t)/vals.length,gap=Math.min(5,bh*.24);ctx.font='10px Microsoft YaHei';for(let i=0;i<=5;i++){const v=max*i/5,x=a.l+(a.r-a.l)*i/5;ctx.strokeStyle='#21344f';ctx.beginPath();ctx.moveTo(x,a.t);ctx.lineTo(x,a.b);ctx.stroke();ctx.fillStyle='#8fa1bf';ctx.textAlign='center';ctx.fillText(Math.abs(v)>=1000000?(v/1000000).toFixed(1)+'M':Math.abs(v)>=1000?(v/1000).toFixed(1)+'K':Number(v.toFixed(2)),x,a.b+16)}vals.forEach((v,i)=>{const y=a.t+i*bh+gap/2,wv=v/max*(a.r-a.l),g=ctx.createLinearGradient(a.l,0,a.r,0);g.addColorStop(0,opt.c1||'#1765d8');g.addColorStop(1,opt.c2||'#6cb4ff');ctx.fillStyle=g;ctx.fillRect(a.l,y,wv,Math.max(2,bh-gap));ctx.fillStyle='#b8c7db';ctx.textAlign='right';ctx.textBaseline='middle';ctx.fillText(labels[i],a.l-8,y+(bh-gap)/2)});ctx.strokeStyle='#55708f';ctx.beginPath();ctx.moveTo(a.l,a.t);ctx.lineTo(a.l,a.b);ctx.lineTo(a.r,a.b);ctx.stroke();ctx.fillStyle='#b7c7dd';ctx.textAlign='center';ctx.fillText(opt.xLabel||'数值',(a.l+a.r)/2,h-8);ctx.save();ctx.translate(15,(a.t+a.b)/2);ctx.rotate(-Math.PI/2);ctx.fillText(opt.yLabel||'类别',0,0);ctx.restore()}
function signedBarChart(id,labels,vals,opt={}){const c=document.getElementById(id);if(!c||c.offsetParent===null||!vals.length)return;const{ctx,w,h}=canvasBox(c),a=chartBase(ctx,w,h,opt,opt.min??Math.min(...vals,-1),opt.max??Math.max(...vals,1)),zero=a.b-(0-a.min)/(a.max-a.min)*(a.b-a.t),bw=(a.r-a.l)/vals.length;vals.forEach((v,i)=>{const y=a.b-(v-a.min)/(a.max-a.min)*(a.b-a.t),x=a.l+i*bw+1;ctx.fillStyle=v>=0?'#ff6678':'#56a3ff';ctx.fillRect(x,Math.min(y,zero),Math.max(1,bw-2),Math.abs(zero-y))});xLabels(ctx,a,labels,{maxLabels:18})}

function drawVisibleCharts(){
 const D=DATA.stats.draws, recent=D.slice(-100), issues=recent.map(x=>x.issue.slice(-3));
 lineChart('trendSumChart',issues,recent.map(x=>x.sum),{xLabel:'期号',yLabel:'红球和值',min:55,max:145});
 lineChart('trendSpanChart',issues,recent.map(x=>x.span),{xLabel:'期号',yLabel:'跨度',min:10,max:32,color:'#9776ff'});
 lineChart('trendOddChart',issues,recent.map(x=>x.odd),{xLabel:'期号',yLabel:'奇数个数',min:0,max:6,color:'#ff6678'});
 lineChart('trendBigChart',issues,recent.map(x=>x.big),{xLabel:'期号',yLabel:'大号个数',min:0,max:6,color:'#40d39c'});
 lineChart('trendConsecutiveChart',issues,recent.map(x=>x.consecutive),{xLabel:'期号',yLabel:'连号组数',min:0,max:4,color:'#ffc85a'});
 lineChart('trendRepeatChart',issues,recent.map(x=>x.repeat),{xLabel:'期号',yLabel:'重号个数',min:0,max:4,color:'#35d3d0'});
 lineChart('trendBlueChart',issues,recent.map(x=>x.blue),{xLabel:'期号',yLabel:'蓝球号码',min:1,max:16,color:'#4c8dff'});
 lineChart('trendAcChart',issues,recent.map(x=>x.ac),{xLabel:'期号',yLabel:'号码分散度',min:2,max:12,color:'#a58bff'});
 multiLineChart('trendZonesChart',issues,[{name:'01–11',values:recent.map(x=>x.zones[0])},{name:'12–22',values:recent.map(x=>x.zones[1])},{name:'23–33',values:recent.map(x=>x.zones[2])}],{xLabel:'期号',yLabel:'每区号码个数',min:0,max:6});
 multiLineChart('trendPositionsChart',issues,[0,1,2,3,4,5].map(i=>({name:'第'+(i+1)+'位',values:recent.map(x=>x.reds[i])})),{xLabel:'期号',yLabel:'红球号码',min:1,max:33});
 groupedBarChart('windowRedChart',DATA.stats.redNumbers.map(x=>pad(x.number)),[{name:'近20期',values:DATA.stats.redNumbers.map(x=>x.recent20)},{name:'近50期',values:DATA.stats.redNumbers.map(x=>x.recent50)},{name:'近100期',values:DATA.stats.redNumbers.map(x=>x.recent100)}],{xLabel:'红球号码',yLabel:'出现次数',maxLabels:17});
 groupedBarChart('windowBlueChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),[{name:'近20期',values:DATA.stats.blueNumbers.map(x=>x.recent20)},{name:'近50期',values:DATA.stats.blueNumbers.map(x=>x.recent50)},{name:'近100期',values:DATA.stats.blueNumbers.map(x=>x.recent100)}],{xLabel:'蓝球号码',yLabel:'出现次数'});
 barChart('trendRedGapChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.gap),{xLabel:'红球号码',yLabel:'遗漏期数',c1:'#8a5428',c2:'#ffc16b'});
 barChart('trendBlueGapChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.gap),{xLabel:'蓝球号码',yLabel:'遗漏期数',c1:'#1765d8',c2:'#6cb4ff'});

 barChart('redCountChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.redCount),{xLabel:'红球号码',yLabel:'历史出现次数',line:DATA.meta.drawCount*6/33});
 barChart('blueCountChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.count),{xLabel:'蓝球号码',yLabel:'历史出现次数',line:DATA.meta.drawCount/16,c1:'#1765d8',c2:'#6cb4ff'});
 groupedBarChart('redBlueCountChart',DATA.stats.redNumbers.slice(0,16).map(x=>pad(x.number)),[{name:'作为红球',values:DATA.stats.redNumbers.slice(0,16).map(x=>x.redCount)},{name:'作为蓝球',values:DATA.stats.blueNumbers.map(x=>x.count)}],{xLabel:'号码',yLabel:'历史出现次数'});
 barChart('yearDrawCountChart',DATA.stats.years.map(x=>String(x.year)),DATA.stats.years.map(x=>x.draws),{xLabel:'年份',yLabel:'开奖期数'});
 barChart('yearFirstWinsChart',DATA.stats.years.map(x=>String(x.year)),DATA.stats.years.map(x=>x.firstWins),{xLabel:'年份',yLabel:'一等奖注数',c1:'#18765d',c2:'#46d6a6'});
 hBarChart('provinceChart',DATA.stats.provinces.slice(0,20).map(x=>x.province),DATA.stats.provinces.slice(0,20).map(x=>x.wins),{xLabel:'一等奖注数',yLabel:'省市'});

 barChart('redRateChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.redRate*100),{xLabel:'红球号码',yLabel:'出现比例（%）',min:15,max:22});
 barChart('redGapHistoryChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.gap),{xLabel:'红球号码',yLabel:'当前遗漏期数'});
 barChart('redAvgGapChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.avgGap),{xLabel:'红球号码',yLabel:'平均遗漏期数',c1:'#18765d',c2:'#46d6a6'});
 barChart('redMaxGapChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.maxGap),{xLabel:'红球号码',yLabel:'最大遗漏期数',c1:'#8a5428',c2:'#ffc16b'});
 barChart('redStreakChart',DATA.stats.redNumbers.map(x=>pad(x.number)),DATA.stats.redNumbers.map(x=>x.maxStreak),{xLabel:'红球号码',yLabel:'最大连续出现次数',min:0,max:6,c1:'#5a3cad',c2:'#a58bff'});
 groupedBarChart('redWeekdayChart',DATA.stats.redNumbers.map(x=>pad(x.number)),[{name:'周二',values:DATA.stats.redNumbers.map(x=>x.Tue)},{name:'周四',values:DATA.stats.redNumbers.map(x=>x.Thu)},{name:'周日',values:DATA.stats.redNumbers.map(x=>x.Sun)}],{xLabel:'红球号码',yLabel:'出现次数',maxLabels:17});
 barChart('blueRateChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.rate*100),{xLabel:'蓝球号码',yLabel:'出现比例（%）',min:4,max:8,c1:'#1765d8',c2:'#6cb4ff'});
 barChart('blueGapHistoryChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.gap),{xLabel:'蓝球号码',yLabel:'当前遗漏期数',c1:'#1765d8',c2:'#6cb4ff'});
 barChart('blueAvgGapChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.avgGap),{xLabel:'蓝球号码',yLabel:'平均遗漏期数',c1:'#18765d',c2:'#46d6a6'});
 barChart('blueMaxGapChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.maxGap),{xLabel:'蓝球号码',yLabel:'最大遗漏期数',c1:'#8a5428',c2:'#ffc16b'});
 barChart('blueStreakChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),DATA.stats.blueNumbers.map(x=>x.maxStreak),{xLabel:'蓝球号码',yLabel:'最大连续出现次数',min:0,max:4,c1:'#5a3cad',c2:'#a58bff'});
 groupedBarChart('blueWeekdayChart',DATA.stats.blueNumbers.map(x=>pad(x.number)),[{name:'周二',values:DATA.stats.blueNumbers.map(x=>x.Tue)},{name:'周四',values:DATA.stats.blueNumbers.map(x=>x.Thu)},{name:'周日',values:DATA.stats.blueNumbers.map(x=>x.Sun)}],{xLabel:'蓝球号码',yLabel:'出现次数'});

 const st=DATA.stats.structures;
 barChart('sumChart',st.sum.map(x=>x.value),st.sum.map(x=>x.count),{xLabel:'红球和值',yLabel:'出现期数',maxLabels:14,c1:'#28518a',c2:'#5bb7ff'});
 barChart('spanChart',st.span.map(x=>x.value),st.span.map(x=>x.count),{xLabel:'红球跨度',yLabel:'出现期数',c1:'#28518a',c2:'#5bb7ff'});
 barChart('oddChart',st.odd.map(x=>x.value),st.odd.map(x=>x.count),{xLabel:'奇数个数',yLabel:'出现期数',c1:'#6d2d7e',c2:'#d480ff'});
 barChart('bigChart',st.big.map(x=>x.value),st.big.map(x=>x.count),{xLabel:'大号个数',yLabel:'出现期数',c1:'#18765d',c2:'#46d6a6'});
 barChart('consecutiveChart',st.consecutive.map(x=>x.value),st.consecutive.map(x=>x.count),{xLabel:'连号组数',yLabel:'出现期数',c1:'#8a5428',c2:'#ffc16b'});
 barChart('repeatChart',st.repeat.map(x=>x.value),st.repeat.map(x=>x.count),{xLabel:'与上期重号个数',yLabel:'出现期数',c1:'#18765d',c2:'#46d6a6'});
 barChart('acChart',st.ac.map(x=>x.value),st.ac.map(x=>x.count),{xLabel:'号码分散度',yLabel:'出现期数',c1:'#5a3cad',c2:'#a58bff'});
 barChart('tailChart',st.tailUnique.map(x=>x.value),st.tailUnique.map(x=>x.count),{xLabel:'尾数种类',yLabel:'出现期数',c1:'#166d7e',c2:'#45d6e8'});
 barChart('primeChart',st.primes.map(x=>x.value),st.primes.map(x=>x.count),{xLabel:'质数个数',yLabel:'出现期数',c1:'#7a4124',c2:'#ffad69'});

 const yrs=DATA.stats.years,yl=yrs.map(x=>String(x.year));
 lineChart('yearSumChart',yl,yrs.map(x=>x.avgSum),{xLabel:'年份',yLabel:'平均和值',min:92,max:107,line:99});
 lineChart('yearSpanChart',yl,yrs.map(x=>x.avgSpan),{xLabel:'年份',yLabel:'平均跨度',min:22,max:27,color:'#9776ff'});
 lineChart('yearOddChart',yl,yrs.map(x=>x.avgOdd),{xLabel:'年份',yLabel:'平均奇数个数',min:2.7,max:3.3,color:'#ff6678'});
 lineChart('yearBigChart',yl,yrs.map(x=>x.avgBig),{xLabel:'年份',yLabel:'平均大号个数',min:2.7,max:3.3,color:'#40d39c'});
 lineChart('yearConsecutiveChart',yl,yrs.map(x=>x.avgConsecutive),{xLabel:'年份',yLabel:'平均连号组数',min:.7,max:1.25,color:'#ffc85a'});
 lineChart('yearRepeatChart',yl,yrs.map(x=>x.avgRepeat),{xLabel:'年份',yLabel:'平均重号个数',min:.8,max:1.35,color:'#35d3d0'});
 barChart('yearSalesChart',yl,yrs.map(x=>x.salesTotal),{xLabel:'年份',yLabel:'销量合计（元）',c1:'#18765d',c2:'#46d6a6'});
 barChart('yearPoolChart',yl,yrs.map(x=>x.avgPool),{xLabel:'年份',yLabel:'平均奖池（元）',c1:'#5a3cad',c2:'#a58bff'});
 groupedBarChart('weekdayCompareChart',DATA.stats.weekdays.map(x=>x.weekday),[{name:'平均和值',values:DATA.stats.weekdays.map(x=>x.avgSum)},{name:'平均跨度',values:DATA.stats.weekdays.map(x=>x.avgSpan)},{name:'平均蓝球',values:DATA.stats.weekdays.map(x=>x.avgBlue)}],{xLabel:'开奖日',yLabel:'平均数值'});

 const prov=DATA.stats.provinces.slice(0,20);
 hBarChart('provinceWinsChart',prov.map(x=>x.province),prov.map(x=>x.wins),{xLabel:'一等奖注数',yLabel:'省市'});
 hBarChart('provinceIssuesChart',[...DATA.stats.provinces].sort((a,b)=>b.issues-a.issues).slice(0,20).map(x=>x.province),[...DATA.stats.provinces].sort((a,b)=>b.issues-a.issues).slice(0,20).map(x=>x.issues),{xLabel:'涉及开奖期数',yLabel:'省市'});
 hBarChart('provinceOutletsChart',[...DATA.stats.provinces].sort((a,b)=>b.uniqueOutlets-a.uniqueOutlets).slice(0,20).map(x=>x.province),[...DATA.stats.provinces].sort((a,b)=>b.uniqueOutlets-a.uniqueOutlets).slice(0,20).map(x=>x.uniqueOutlets),{xLabel:'唯一中奖网点数',yLabel:'省市'});
 const pt=[...DATA.stats.provinces].sort((a,b)=>b.avgTicketAmount-a.avgTicketAmount).slice(0,20);hBarChart('provinceTicketChart',pt.map(x=>x.province),pt.map(x=>x.avgTicketAmount),{xLabel:'平均票面金额（元）',yLabel:'省市'});
 const ro=[...DATA.stats.outlets].sort((a,b)=>b.issues-a.issues||b.wins-a.wins).slice(0,20);hBarChart('repeatOutletChart',ro.map(x=>x.outlet),ro.map(x=>x.issues),{xLabel:'涉及开奖期数',yLabel:'网点编号',left:115});
 const pairs=DATA.stats.pairs.slice(0,20),pairLift=[...DATA.stats.pairs].sort((a,b)=>b.lift-a.lift).slice(0,20),triples=DATA.stats.triples.slice(0,20),tripleLift=[...DATA.stats.triples].sort((a,b)=>b.lift-a.lift).slice(0,20);
 hBarChart('pairCountChart',pairs.map(x=>x.pair),pairs.map(x=>x.count),{xLabel:'同时出现次数',yLabel:'双号码'});
 hBarChart('pairLiftChart',pairLift.map(x=>x.pair),pairLift.map(x=>x.lift),{xLabel:'共同出现倍数',yLabel:'双号码'});
 hBarChart('tripleCountChart',triples.map(x=>x.triple),triples.map(x=>x.count),{xLabel:'同时出现次数',yLabel:'三号码',left:115});
 hBarChart('tripleLiftChart',tripleLift.map(x=>x.triple),tripleLift.map(x=>x.lift),{xLabel:'共同出现倍数',yLabel:'三号码',left:115});

 lineChart('redBacktestChart',DATA.backtest.redIssues,DATA.backtest.redRolling,{xLabel:'回测期号',yLabel:'红球前6名平均命中数',min:.7,max:1.7,line:DATA.backtest.redRandomTop6});
 lineChart('blueBacktestChart',DATA.backtest.blueIssues,DATA.backtest.blueRolling,{xLabel:'回测期号',yLabel:'蓝球第1名命中率',min:0,max:.22,line:DATA.backtest.blueRandomTop1,color:'#40d39c'});
 signedBarChart('redZChart',DATA.audit.redDeviations.map(x=>pad(x.number)),DATA.audit.redDeviations.map(x=>x.z),{xLabel:'红球号码',yLabel:'偏离程度',min:-3.5,max:3.5});
}

window.addEventListener('resize',()=>setTimeout(drawVisibleCharts,100));
initMeta();renderSchemes();renderProbability();renderDimensions();renderProfessional();initHistory();drawVisibleCharts();



(function(){
  // Restore the V2 logo exactly.
  const brand=document.querySelector('.brand');
  if(brand){
    const old=brand.querySelector('.six-logo');
    if(old){const logo=document.createElement('div');logo.className='logo';logo.textContent='6+1';old.replaceWith(logo)}
  }

  // Restore the former right-side quality area as two business-focused blocks.
  const mainPanel=document.querySelector('#forecast .forecast-main');
  if(mainPanel && !document.querySelector('.forecast-hero')){
    const hero=document.createElement('div');hero.className='forecast-hero';
    mainPanel.parentNode.insertBefore(hero,mainPanel);hero.appendChild(mainPanel);
    const aside=document.createElement('aside');aside.className='result-side';aside.innerHTML='<div class="panel side-block"><div class="side-title">最近10期开奖</div><div class="recent-window"><div class="recent-track" id="recentTrack"></div></div></div><div class="panel side-block"><div class="side-title">候选组合</div><div class="alt-candidates" id="altCandidates"></div></div>';
    hero.appendChild(aside);
  }
  function smallBalls(reds,blue){return reds.map(n=>'<span class="tiny-result-ball">'+pad(n)+'</span>').join('')+'<span class="tiny-result-ball blue">'+pad(blue)+'</span>'}
  const recent=DATA.stats.draws.slice(-10).reverse();
  const recentHTML=recent.map(d=>'<div class="recent-row"><span class="issue">'+d.issue+'<br>'+d.date.slice(5)+'</span><span class="recent-balls">'+smallBalls(d.reds,d.blue)+'</span><span></span></div>').join('');
  const rt=document.getElementById('recentTrack');if(rt)rt.innerHTML=recentHTML+recentHTML;
  const ac=document.getElementById('altCandidates');if(ac)ac.innerHTML=DATA.prediction.topCandidates.slice(1,4).map((c,i)=>'<div class="alt-candidate"><div class="alt-candidate-head"><b>候选'+(i+1)+'</b><span>2元</span></div><div class="alt-candidate-balls">'+c.reds.map(n=>'<span class="mini">'+pad(n)+'</span>').join('')+'<span class="mini blue">'+pad(c.blue)+'</span></div></div>').join('');

  // Modal shell.
  if(!document.getElementById('reasonModal')){
    document.body.insertAdjacentHTML('beforeend','<div class="reason-modal" id="reasonModal"><div class="reason-dialog" role="dialog" aria-modal="true" aria-labelledby="reasonTitle"><div class="reason-head"><h2 id="reasonTitle">预测原因</h2><button class="reason-close" id="reasonClose" aria-label="关闭">×</button></div><div id="reasonBody"></div></div></div>');
  }
  function reasonHTML(s){
    const red=s.redDetails.map(r=>'<div class="reason-row-v6"><span class="num-dot">'+pad(r.number)+'</span><div class="main"><b>'+esc(r.role)+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.prob)+'</div></div>').join('');
    const blue=s.blueDetails.map(r=>'<div class="reason-row-v6"><span class="num-dot blue">'+pad(r.number)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.prob)+'</div></div>').join('');
    const added=s.additions&&s.additions.length?'<p class="muted">本组在61核心红球上增加：'+s.additions.map(pad).join('、')+'。</p>':'';
    return '<div class="reason-summary"><span class="scheme-code">'+s.code+'</span><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">购买金额：'+(s.bets*2)+'元</span></div>'+added+'<div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
  }
  function openReason(code){const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;document.getElementById('reasonTitle').textContent=s.code+'组合预测原因';document.getElementById('reasonBody').innerHTML=reasonHTML(s);document.getElementById('reasonModal').classList.add('open');document.body.style.overflow='hidden'}
  function closeReason(){document.getElementById('reasonModal').classList.remove('open');document.body.style.overflow=''}
  document.getElementById('reasonClose').onclick=closeReason;
  document.getElementById('reasonModal').onclick=e=>{if(e.target.id==='reasonModal')closeReason()};
  document.addEventListener('keydown',e=>{if(e.key==='Escape')closeReason()});

  // Replace scheme rendering: show price, remove technical KPI blocks, use modal details.
  schemeCard=function(s,primary=false){
    const detailButton=primary?'':'<div class="actions" style="margin-top:12px"><button class="btn btn-blue scheme-reason-btn" data-code="'+s.code+'">查看'+s.code+'预测原因</button></div>';
    return '<div class="'+(primary?'':'scheme-card')+'"><div class="scheme-card-head"><div><span class="scheme-code">'+s.code+'</span><span class="scheme-name">'+esc(s.label)+'</span></div></div><div class="ball-row">'+ballsHTML(s.reds,s.blues)+'</div><div class="scheme-price"><span>购买金额</span><b>'+s.bets*2+'元</b><small>'+s.bets+'注</small></div>'+detailButton+'</div>';
  };
  bindSchemeButtons=function(){$$('.scheme-reason-btn').forEach(b=>b.onclick=()=>openReason(b.dataset.code))};
  renderSchemes=function(animate=false){const schemes=DATA.prediction.schemes;$('#primaryScheme').innerHTML=schemeCard(schemes[0],true);$('#schemeGrid').innerHTML=schemes.slice(1).map(s=>schemeCard(s)).join('');bindSchemeButtons();if(animate){$$('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}};
  renderSchemes();
  {const el=document.getElementById('generateAllBtn');if(el)el.onclick=()=>renderSchemes(true);}
  {const el=document.getElementById('primaryDetailBtn');if(el)el.onclick=()=>openReason('61');}

  // Rebuild probability area with number + next-draw probability only.
  renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    red.className='probability-board';blue.className='probability-board';
    red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('');
    blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('');
  };
  renderProbability();
  const redTitle=document.querySelector('#redHeat')?.closest('.panel')?.querySelector('h3');if(redTitle)redTitle.textContent='红球';
  const blueTitle=document.querySelector('#blueHeat')?.closest('.panel')?.querySelector('h3');if(blueTitle)blueTitle.textContent='蓝球';

  // Professional page becomes a direct glossary.
  const navProf=document.querySelector('[data-page="professional"]');if(navProf)navProf.textContent='术语说明';
  const terms=[
    ['进入下一期概率','系统根据历史数据计算的单个号码在下一期出现的估计比例。'],['复式','一次选择超过6个红球或超过1个蓝球，系统自动拆成多注标准号码。'],['注数','一组标准的6个红球加1个蓝球算1注。'],['61 / 62 / 71 / 72 / 81 / 82','前一位表示红球个数，后一位表示蓝球个数。'],['近20期','只统计最近20次开奖。'],['历史次数','该号码在全部历史开奖中出现的总次数。'],['遗漏期数','从该号码上次出现到现在，连续没有出现的期数。'],['平均遗漏','历史上两次出现之间，平均间隔多少期。'],['最大遗漏','历史记录中连续没有出现的最长间隔。'],['和值','6个红球相加后的总数。'],['跨度','最大红球减去最小红球。'],['奇偶比','6个红球中奇数和偶数的数量比例。'],['大小比','1—16为小号，17—33为大号，两类号码的数量比例。'],['三区比','01—11、12—22、23—33三个区间各有几个红球。'],['连号','同一期中相邻的号码，例如09和10。'],['重号','与上一期重复出现的红球。'],['尾数','号码个位数，例如09和19的尾数都是9。'],['号码分散度（AC值）','用号码之间不同差值的多少，表示一组号码分散还是集中。'],['共现次数','两个或三个号码在同一期一起出现过多少次。'],['共同出现倍数','一组号码实际一起出现的频率，相对各自独立出现时高出多少。'],['PMI','衡量两个号码一起出现是否比随机组合更频繁。'],['Z值','某个号码的历史次数距离平均水平有多远。'],['卡方检验','检查所有号码的历史次数是否明显偏离平均分布。'],['p值','用来衡量观察到的差异由随机波动造成的可能程度。'],['相关系数','衡量两类数据一起变化的方向和强弱。'],['时间切分回测','只用过去数据预测后面的期次，避免提前看到答案。'],['逻辑回归','根据多个历史因素计算号码出现概率的算法。'],['梯度提升','通过多轮修正错误来寻找复杂关系的算法。'],['极端随机树','用大量随机决策树综合计算结果的算法。'],['对数损失','衡量概率预测是否准确；越小通常越好。']
  ];
  const prof=document.getElementById('professional');if(prof)prof.innerHTML='<div class="section-head"><div><h2>术语说明</h2></div></div><div class="term-grid">'+terms.map(t=>'<article class="term-card"><h3>'+esc(t[0])+'</h3><p>'+esc(t[1])+'</p></article>').join('')+'</div>';

  // Generic tooltip for every Canvas chart renderer.
  if(!document.getElementById('chartTooltip'))document.body.insertAdjacentHTML('beforeend','<div class="chart-tooltip" id="chartTooltip"></div>');
  const tip=document.getElementById('chartTooltip');
  function vfmt(v,label){if(v==null||Number.isNaN(Number(v)))return String(v);const n=Number(v);if((label||'').includes('元'))return n.toLocaleString('zh-CN',{maximumFractionDigits:0})+'元';if((label||'').includes('%')||(label||'').includes('比例')||(label||'').includes('命中率'))return n.toFixed(2)+(label.includes('%')?'%':'');if(Number.isInteger(n))return n.toLocaleString('zh-CN');return n.toFixed(Math.abs(n)<1?4:2)}
  function attachTooltip(id,labels,series,opt={},orientation='x',mode='bar'){
    const c=document.getElementById(id);if(!c)return;c.style.cursor='crosshair';
    c.onmousemove=function(e){const r=c.getBoundingClientRect(),mx=e.clientX-r.left,my=e.clientY-r.top;let idx=-1;
      if(orientation==='y'){const top=18,bottom=48,ph=r.height-top-bottom;if(my<top||my>top+ph)return tip.classList.remove('show');idx=Math.floor((my-top)/ph*labels.length)}
      else{const left=opt.left||68,right=18,top=18,bottom=opt.bottom||62,pw=r.width-left-right;if(mx<left||mx>left+pw||my<top||my>r.height-bottom)return tip.classList.remove('show');const ratio=(mx-left)/pw;idx=mode==='line'?Math.round(ratio*(labels.length-1)):Math.floor(ratio*labels.length)}
      idx=Math.max(0,Math.min(labels.length-1,idx));let html='<b>'+esc(String(labels[idx]))+'</b>';series.forEach(s=>{html+='<span><i>'+esc(s.name||opt.yLabel||'数值')+'</i><em>'+vfmt(s.values[idx],s.name||opt.yLabel||'')+'</em></span>'});tip.innerHTML=html;tip.classList.add('show');const tw=tip.offsetWidth,th=tip.offsetHeight;tip.style.left=Math.min(window.innerWidth-tw-10,e.clientX+14)+'px';tip.style.top=Math.min(window.innerHeight-th-10,e.clientY+14)+'px'};
    c.onmouseleave=()=>tip.classList.remove('show');
  }
  const oldBar=barChart,oldLine=lineChart,oldGroup=groupedBarChart,oldMulti=multiLineChart,oldH=hBarChart,oldSigned=signedBarChart;
  barChart=function(id,labels,vals,opt={}){oldBar(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','bar')};
  lineChart=function(id,labels,vals,opt={}){oldLine(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','line')};
  groupedBarChart=function(id,labels,series,opt={}){oldGroup(id,labels,series,opt);attachTooltip(id,labels,series,opt,'x','bar')};
  multiLineChart=function(id,labels,series,opt={}){oldMulti(id,labels,series,opt);attachTooltip(id,labels,series,opt,'x','line')};
  hBarChart=function(id,labels,vals,opt={}){oldH(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.xLabel||'数值',values:vals}],opt,'y','bar')};
  signedBarChart=function(id,labels,vals,opt={}){oldSigned(id,labels,vals,opt);attachTooltip(id,labels,[{name:opt.yLabel||'数值',values:vals}],opt,'x','bar')};
  drawVisibleCharts();
})();



(function(){
  const escV7=s=>String(s).replace(/[&<>\"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[m]));

  // 1) Scheme cards: price + bet count in the top-right; remove label headers.
  window.schemeCard=function(s,primary=false){
    const detailButton=primary?'':'<div class="actions" style="margin-top:12px"><button class="btn btn-blue scheme-reason-btn" data-code="'+s.code+'">查看'+s.code+'预测原因</button></div>';
    return '<div class="'+(primary?'scheme-v7 primary-scheme-content':'scheme-card scheme-v7')+'">'
      +'<div class="scheme-card-head"><div><span class="scheme-code">'+s.code+'</span></div></div>'
      +'<div class="scheme-cost-top"><b>'+(s.bets*2)+'元</b><span>'+s.bets+'注</span></div>'
      +'<div class="ball-row">'+ballsHTML(s.reds,s.blues)+'</div>'+detailButton+'</div>';
  };
  const bindExistingSchemeButtons=window.bindSchemeButtons;
  window.renderSchemes=function(animate=false){
    const schemes=DATA.prediction.schemes;
    document.getElementById('primaryScheme').innerHTML=window.schemeCard(schemes[0],true);
    document.getElementById('schemeGrid').innerHTML=schemes.slice(1).map(s=>window.schemeCard(s)).join('');
    if(typeof bindExistingSchemeButtons==='function')bindExistingSchemeButtons();
    if(animate){document.querySelectorAll('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}
  };
  window.renderSchemes();

  // 2) Four additional 61 candidates.
  const sideTitle=[...document.querySelectorAll('.side-title')].find(x=>x.textContent.includes('61组合'));
  if(sideTitle)sideTitle.textContent='候选组合';
  const ac=document.getElementById('altCandidates');
  if(ac){
    ac.innerHTML=DATA.prediction.topCandidates.slice(1,5).map((c,i)=>'<div class="alt-candidate"><div class="alt-candidate-head"><b>候选'+(i+1)+'</b><span>2元 · 1注</span></div><div class="alt-candidate-balls">'+c.reds.map(n=>'<span class="mini">'+pad(n)+'</span>').join('')+'<span class="mini blue">'+pad(c.blue)+'</span></div></div>').join('');
  }

  // 3) Full feature, dimension and terminology documentation.
  const features=[
    ['生成六组号码','点击一次，同时重新显示61、62、71、72、81、82六种投注方案。'],
    ['61方案','选择6个红球和1个蓝球，共1注，购买金额2元。'],
    ['62方案','选择6个红球和2个蓝球，共2注，购买金额4元。'],
    ['71方案','选择7个红球和1个蓝球，自动拆成7注，购买金额14元。'],
    ['72方案','选择7个红球和2个蓝球，自动拆成14注，购买金额28元。'],
    ['81方案','选择8个红球和1个蓝球，自动拆成28注，购买金额56元。'],
    ['82方案','选择8个红球和2个蓝球，自动拆成56注，购买金额112元。'],
    ['展开其它5组','默认只突出61方案；点击后查看62、71、72、81、82。'],
    ['查看预测原因','点击任意方案的按钮，在弹窗中查看每个号码的概率、近期次数、遗漏和排名。'],
    ['所有号码概率','列出01—33红球和01—16蓝球进入下一期的估计概率。'],
    ['最近10期开奖','页面一右侧自动循环展示最近10期开奖的期号、日期和号码。'],
    ['61另外4组候选','除主61方案外，再展示4组61备选号码，每组2元、1注。'],
    ['趋势分析','用最近期次的折线图和对比图观察号码形态如何变化。'],
    ['历史统计','从全部历史数据统计号码、组合、时间、地区、网点和搭配关系。'],
    ['图表提示','鼠标移到图表数据上，显示该位置对应的名称和具体数值。'],
    ['历史明细搜索','在全部开奖记录中按期号或号码查找对应记录。'],
    ['历史明细分页','开奖记录较多时分成多页浏览，避免一次显示过多。'],
    ['统计表搜索','省市、网点、号码搭配等明细表可按关键词筛选。'],
    ['CSV导出','把当前统计表或筛选结果导出为CSV文件，便于继续分析。'],
    ['术语说明','集中解释页面功能、分析维度、算法和统计名词。']
  ];

  const dimensions=[
    ['趋势分析','红球和值走势','观察每期6个红球总和是升高、降低还是在某一区间来回变化。'],
    ['趋势分析','红球跨度走势','观察每期最大红球与最小红球之间的距离变化。'],
    ['趋势分析','奇数数量走势','观察每期6个红球中奇数数量的变化。'],
    ['趋势分析','大号数量走势','观察17—33红球在每期中出现几个。'],
    ['趋势分析','连号组数走势','观察相邻号码同时出现的组数变化。'],
    ['趋势分析','与上期重号数量走势','观察本期与上一期重复红球的数量。'],
    ['趋势分析','蓝球号码走势','按开奖顺序观察蓝球号码高低变化。'],
    ['趋势分析','号码分散程度走势','观察每组红球是更集中还是更分散。'],
    ['趋势分析','三区号码数量走势','比较01—11、12—22、23—33三个区间每期各出几个。'],
    ['趋势分析','红球6个位置走势','把每期红球从小到大排列，分别观察第1到第6个位置的变化。'],
    ['趋势分析','红球近20、50、100期次数','比较短期、中期和较长期的号码活跃程度。'],
    ['趋势分析','蓝球近20、50、100期次数','比较蓝球在不同时间窗口中的出现次数。'],
    ['趋势分析','红球当前遗漏','查看每个红球距离上次出现已经过了多少期。'],
    ['趋势分析','蓝球当前遗漏','查看每个蓝球距离上次出现已经过了多少期。'],

    ['历史统计','红球历史出现次数','统计01—33每个号码在全部期次中作为红球出现多少次。'],
    ['历史统计','蓝球历史出现次数','统计01—16每个号码在全部期次中作为蓝球出现多少次。'],
    ['历史统计','01—16红蓝对比','比较同一个数字在红球和蓝球位置分别出现多少次。'],
    ['历史统计','各年开奖期数','统计每个年份纳入分析的开奖数量。'],
    ['历史统计','各年一等奖注数','比较不同年份一等奖总注数。'],
    ['历史统计','一等奖省市排名','比较各省市历史一等奖注数。'],
    ['号码细分','红球出现比例','把红球出现次数换算成占全部开奖期数的比例。'],
    ['号码细分','红球当前、平均、最大遗漏','从当前间隔、历史平均间隔和历史最长间隔三个层次观察红球。'],
    ['号码细分','红球最大连续出现次数','统计某红球最多连续多少期都出现。'],
    ['号码细分','红球开奖日分布','比较每个红球在周二、周四、周日的出现次数。'],
    ['号码细分','蓝球出现比例','把蓝球出现次数换算成占全部开奖期数的比例。'],
    ['号码细分','蓝球当前、平均、最大遗漏','从当前、平均和最长间隔三个层次观察蓝球。'],
    ['号码细分','蓝球最大连续出现次数','统计某蓝球最多连续多少期都出现。'],
    ['号码细分','蓝球开奖日分布','比较每个蓝球在周二、周四、周日的出现次数。'],
    ['组合形态','和值分布','统计历史上不同红球总和值区间分别出现多少次。'],
    ['组合形态','跨度分布','统计历史上不同最大最小号码差值出现多少次。'],
    ['组合形态','奇数个数分布','统计0—6个奇数的各种组合出现频率。'],
    ['组合形态','大号个数分布','统计0—6个大号的各种组合出现频率。'],
    ['组合形态','连号组数分布','统计没有连号、1组连号或多组连号分别出现多少次。'],
    ['组合形态','重号数分布','统计与上一期重复0—6个红球的次数。'],
    ['组合形态','号码分散度分布','统计不同分散程度的红球组合出现次数。'],
    ['组合形态','尾数种类分布','统计一组红球包含多少种不同个位数。'],
    ['组合形态','质数个数分布','统计每期红球中包含几个质数。'],
    ['时间细分','年度平均和值','比较每年红球和值的平均水平。'],
    ['时间细分','年度平均跨度','比较每年红球跨度的平均水平。'],
    ['时间细分','年度平均奇数和大号数','比较各年奇偶与大小结构。'],
    ['时间细分','年度平均连号和重号','比较各年连号、重号的平均数量。'],
    ['时间细分','年度销量和平均奖池','比较不同年份的销售金额和奖池规模。'],
    ['时间细分','开奖日平均数据','比较周二、周四、周日的平均和值、跨度和蓝球。'],
    ['地区网点','省市一等奖注数','按省市累计一等奖注数并排序。'],
    ['地区网点','省市涉及期数','统计每个省市在多少个不同开奖期出现一等奖。'],
    ['地区网点','省市唯一中奖网点','比较各省市出现过的一等奖销售网点数量。'],
    ['地区网点','省市平均票面金额','比较一等奖记录中的平均票面购买金额。'],
    ['地区网点','跨期中奖网点','找出在多个开奖期中重复出现的一等奖销售网点。'],
    ['号码搭配','双号码共同出现次数','统计任意两个红球在同一期一起出现的次数。'],
    ['号码搭配','双号码共同出现倍数','比较两个号码实际同开频率与按各自频率推算值的差距。'],
    ['号码搭配','三号码共同出现次数','统计任意三个红球在同一期一起出现的次数。'],
    ['号码搭配','三号码共同出现倍数','比较三个号码实际同开频率与独立推算值的差距。'],
    ['开奖明细','全部开奖记录','逐期列出期号、日期、6个红球、蓝球、销量、奖池和奖项信息。']
  ];

  const terms=[
    ['进入下一期概率','系统根据历史数据和当前模型估计单个号码在下一期开奖中出现的比例。'],
    ['复式','选择多于6个红球或多于1个蓝球，系统自动拆成多注标准号码。'],
    ['注数','一组标准的6个红球加1个蓝球算1注，每注2元。'],
    ['近20 / 50 / 100期','分别用最近20、50、100次开奖统计短期、中期和较长期变化。'],
    ['遗漏期数','从号码上次出现到现在，连续没有出现的期数。'],
    ['平均遗漏','历史上同一号码两次出现之间的平均间隔。'],
    ['最大遗漏','历史记录中同一号码连续没有出现的最长间隔。'],
    ['和值','一期6个红球相加后的总数。'],
    ['跨度','最大红球号码减去最小红球号码。'],
    ['奇偶比','一期红球中奇数和偶数的数量比例。'],
    ['大小比','01—16算小号，17—33算大号，两类号码的数量比例。'],
    ['三区比','01—11、12—22、23—33三个号码区间的数量比例。'],
    ['连号','同一期中出现相邻号码，例如09和10。'],
    ['重号','与上一期重复出现的红球。'],
    ['尾数','号码的个位数，例如09和19的尾数都是9。'],
    ['号码分散度（AC值）','根据红球之间不同差值的数量衡量一组号码的分散程度。'],
    ['共现次数','两个或三个号码在同一期一起出现过多少次。'],
    ['共同出现倍数','实际共同出现频率相对按各号码独立频率推算值的倍数。'],
    ['PMI','衡量两个号码一起出现是否高于按各自频率推算的水平。'],
    ['Z值','表示某个号码的历史次数距离全部号码平均水平有多远。'],
    ['卡方检验','检查全部号码的历史次数是否明显偏离平均分布。'],
    ['p值','表示当前差异由随机波动产生的可能程度。'],
    ['相关系数','衡量两类数据一起变化的方向和强弱，范围通常为-1到1。'],
    ['时间切分回测','只用过去数据预测后面的期开奖，避免提前看到结果。'],
    ['逻辑回归','把多个历史因素合并成号码出现概率的算法。'],
    ['梯度提升','通过多轮修正前一轮错误来寻找复杂关系的算法。'],
    ['极端随机树','用许多随机决策树综合计算号码分数的算法。'],
    ['对数损失','衡量概率预测质量的指标，通常越小越好。'],
    ['模型排名','把所有号码按估计概率从高到低排列后的名次。'],
    ['Canvas图表','网页直接绘制的图表，支持坐标轴、数据点和悬停提示。']
  ];

  const prof=document.getElementById('professional');
  if(prof){
    prof.innerHTML=''
      +'<section class="glossary-section"><div class="glossary-section-head"><div><h2>功能说明</h2><p>解释系统页面上的全部主要功能</p></div></div><div class="feature-grid">'+features.map(x=>'<article class="feature-card"><h3>'+escV7(x[0])+'</h3><p>'+escV7(x[1])+'</p></article>').join('')+'</div></section>'
      +'<section class="glossary-section"><div class="glossary-section-head"><div><h2>分析维度说明</h2><p>解释趋势分析和历史统计拆分出的每个角度</p></div></div><div class="dimension-help-grid">'+dimensions.map(x=>'<article class="dimension-help-card"><span class="group-tag">'+escV7(x[0])+'</span><h3>'+escV7(x[1])+'</h3><p>'+escV7(x[2])+'</p></article>').join('')+'</div></section>'
      +'<section class="glossary-section"><div class="glossary-section-head"><div><h2>术语说明</h2><p>页面中使用到的号码、统计和算法名词</p></div></div><div class="term-grid-v7">'+terms.map(x=>'<article class="term-card-v7"><h3>'+escV7(x[0])+'</h3><p>'+escV7(x[1])+'</p></article>').join('')+'</div></section>';
  }
})();



(function(){
  const esc8=s=>String(s).replace(/[&<>\"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[m]||m));
  const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
  const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));

  // Add 93: 9 red + 3 blue = C(9,6) × 3 = 252 bets, 504 yuan.
  if(!DATA.prediction.schemes.some(x=>x.code==='93')){
    const core61=DATA.prediction.schemes.find(x=>x.code==='61');
    const reds=[2,6,9,10,13,18,22,25,28];
    const blues=[11,3,15];
    DATA.prediction.schemes.push({
      code:'93',label:'9个红球 + 3个蓝球',note:'扩大红球与蓝球覆盖范围',reds,blues,
      redCount:9,blueCount:3,bets:252,additions:reds.filter(n=>!core61.reds.includes(n)),
      redDetails:reds.map(n=>{const x=redMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.redCount,recent20:x.recent20,recent50:x.recent50,gap:x.gap,status:x.status,role:core61.reds.includes(n)?'核心':'扩展'}}),
      blueDetails:blues.map(n=>{const x=blueMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.count,recent20:x.recent20,gap:x.gap}})
    });
  }

  function schemeCardV8(s,primary=false){
    return '<div class="'+(primary?'scheme-v8 primary-scheme-content':'scheme-card scheme-v8')+'" data-scheme="'+s.code+'">'
      +'<div class="scheme-card-head"><div class="scheme-cost-flow"><b>'+(s.bets*2)+'元</b><span>'+s.bets+'注</span></div></div>'
      +'<div class="ball-row" title="点击号码查看预测原因">'+ballsHTML(s.reds,s.blues)+'</div></div>';
  }
  window.schemeCard=schemeCardV8;
  window.renderSchemes=function(animate=false){
    const schemes=DATA.prediction.schemes;
    const primary=document.getElementById('primaryScheme'),grid=document.getElementById('schemeGrid');
    if(primary)primary.innerHTML=schemeCardV8(schemes[0],true);
    if(grid)grid.innerHTML=schemes.slice(1).map(s=>schemeCardV8(s,false)).join('');
    if(animate){document.querySelectorAll('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}
  };
  window.renderSchemes();

  // Remove obsolete buttons and rename accordion controls.
  ['generateAllBtn','primaryDetailBtn'].forEach(id=>document.getElementById(id)?.remove());
  const addonBtn=document.getElementById('openAddonsBtn');if(addonBtn)addonBtn.textContent='复式预测组';
  const addonSummary=document.querySelector('#addonMaster>summary');if(addonSummary)addonSummary.textContent='复式预测组';

  // Candidate block and recent-draw label.
  const candidateTitle=[...document.querySelectorAll('.side-title')].find(x=>x.textContent.includes('候选')||x.textContent.includes('61组合'));
  if(candidateTitle)candidateTitle.textContent='候选组合';
  const recentTitle=[...document.querySelectorAll('.side-title')].find(x=>x.textContent.includes('最近10期开奖'));
  if(recentTitle)recentTitle.innerHTML='最近10期开奖';

  // Probability page title.
  const probTitle=[...document.querySelectorAll('#forecast .section-head h2')].find(x=>x.textContent.includes('概率'));
  if(probTitle)probTitle.textContent='下期号码概率';

  // Click a red/blue number to open its scheme reason modal.
  function reasonHTML8(s){
    const red=s.reds.map(n=>{const r=redMap.get(n);const role=(s.code==='61'||DATA.prediction.schemes[0].reds.includes(n))?'核心':'扩展';return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+role+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const added=s.additions&&s.additions.length?'<p class="muted">相对核心组合增加红球：'+s.additions.map(pad).join('、')+'。</p>':'';
    return '<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div>'+added+'<div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
  }
  function openReason8(code){
    const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;
    const title=document.getElementById('reasonTitle'),body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal');
    if(title)title.textContent='预测原因';if(body)body.innerHTML=reasonHTML8(s);if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
  }
  ['primaryScheme','schemeGrid'].forEach(id=>{
    const root=document.getElementById(id);if(!root)return;
    root.onclick=e=>{const ball=e.target.closest('.ball');if(!ball)return;const card=ball.closest('[data-scheme]');if(card)openReason8(card.dataset.scheme)};
  });

  // Make the draw red-ball column explicitly width-aware.
  const drawHead=document.querySelector('#drawTable th:nth-child(4)');if(drawHead)drawHead.textContent='红球（6个）';

  // Update glossary to match the current product.
  const featureGrid=document.querySelector('.feature-grid');
  if(featureGrid){
    [...featureGrid.children].forEach(card=>{
      const h=card.querySelector('h3'),p=card.querySelector('p');if(!h||!p)return;
      if(h.textContent==='生成六组号码')card.remove();
      else if(h.textContent==='展开其它5组'){h.textContent='复式预测组';p.textContent='展开查看62、71、72、81、82和93复式号码。';}
      else if(h.textContent==='查看预测原因'){h.textContent='点击号码查看预测原因';p.textContent='点击任意预测组中的红球或蓝球，在弹窗中查看该组每个号码的概率、近期次数、遗漏和排名。';}
      else if(h.textContent.includes('所有号码概率')){h.textContent='下期号码概率';p.textContent='按号码列出01—33红球和01—16蓝球进入下一期的估计概率，每行显示10个。';}
      else if(h.textContent.includes('61另外4组候选')){h.textContent='候选组合';p.textContent='除主预测组外，再展示4组61备选号码，每组2元、1注。';}
      else if(h.textContent==='最近10期开奖'){p.textContent='页面一右侧展示最近10期开奖的期号、日期和号码。';}
    });
    if(![...featureGrid.querySelectorAll('h3')].some(x=>x.textContent==='93方案')){
      featureGrid.insertAdjacentHTML('beforeend','<article class="feature-card"><h3>93方案</h3><p>选择9个红球和3个蓝球，自动拆成252注，购买金额504元。</p></article>');
    }
  }
  document.querySelectorAll('.term-card-v7 h3').forEach(h=>{
    if(h.textContent.includes('61 / 62 / 71 / 72 / 81 / 82')){h.textContent='61 / 62 / 71 / 72 / 81 / 82 / 93';const p=h.nextElementSibling;if(p)p.textContent='前一位表示红球个数，后一位表示蓝球个数；93表示9个红球和3个蓝球。';}
  });
})();



(function(){
  // Restore the exact target issue/date even if an earlier extension script failed.
  const title=document.getElementById('forecastTitle');
  if(title)title.textContent='第 '+DATA.meta.forecastIssue+' 期 · '+DATA.meta.forecastDate;
  const next=document.getElementById('nextIssue');if(next)next.textContent=DATA.meta.forecastIssue;
  const end=document.getElementById('endIssue');if(end)end.textContent=DATA.meta.dataEndIssue;

  // Put prediction numbers and purchase amount on the same row.
  function card(s,primary){
    return '<div class="'+(primary?'scheme-v9 primary-scheme-content':'scheme-card scheme-v9')+'" data-scheme="'+s.code+'">'
      +'<div class="scheme-line"><div class="ball-row" title="点击号码查看预测原因">'+ballsHTML(s.reds,s.blues)+'</div>'
      +'<div class="scheme-cost-inline"><b>'+(s.bets*2)+'元</b><span>'+s.bets+'注</span></div></div></div>';
  }
  window.schemeCard=card;
  window.renderSchemes=function(animate){
    const schemes=DATA.prediction.schemes;
    const primary=document.getElementById('primaryScheme');
    const grid=document.getElementById('schemeGrid');
    if(primary)primary.innerHTML=card(schemes[0],true);
    if(grid)grid.innerHTML=schemes.slice(1).map(s=>card(s,false)).join('');
    if(animate){document.querySelectorAll('#primaryScheme .ball,#schemeGrid .ball').forEach((el,i)=>{el.style.opacity='0';el.style.transform='scale(.5)';setTimeout(()=>{el.style.transition='.25s';el.style.opacity='1';el.style.transform='scale(1)'},i*35)})}
  };
  window.renderSchemes(false);

  // Restore all 33 red and 16 blue next-draw probabilities.
  window.renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    if(red){red.className='probability-board';red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('')}
    if(blue){blue.className='probability-board';blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span><span class="prob-label">进入下一期</span></span></div>').join('')}
  };
  window.renderProbability();

  // Rebind number-click reason opening after the cards were rebuilt.
  function openCurrentReason(code){
    const s=DATA.prediction.schemes.find(x=>x.code===code);if(!s)return;
    const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
    const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));
    const red=s.reds.map(n=>{const r=redMap.get(n);return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+(DATA.prediction.schemes[0].reds.includes(n)?'核心':'扩展')+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
    const body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal'),heading=document.getElementById('reasonTitle');
    if(heading)heading.textContent='预测原因';
    if(body)body.innerHTML='<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div><div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
    if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
  }
  ['primaryScheme','schemeGrid'].forEach(id=>{const root=document.getElementById(id);if(root)root.onclick=e=>{const b=e.target.closest('.ball');if(!b)return;const c=b.closest('[data-scheme]');if(c)openCurrentReason(c.dataset.scheme)}});

  // Force a redraw now that all chart wrapper/tooltip scripts have loaded.
  if(typeof drawVisibleCharts==='function')setTimeout(drawVisibleCharts,80);
})();



(function(){
  document.title='智选';
  const brandName=document.querySelector('.brand-copy b');if(brandName)brandName.textContent='智选';

  // Remove only the page-level duplicate title; keep the navigation name and chart group titles.
  const dimensionHead=document.querySelector('#dimensions > .section-head:first-child');
  if(dimensionHead)dimensionHead.remove();

  // Remove the separate top button while keeping the details/summary below fully expandable.
  const addonButton=document.getElementById('openAddonsBtn');
  if(addonButton){const actions=addonButton.closest('.actions');if(actions)actions.remove();else addonButton.remove();}

  // Convert the final multiple group from 9+3 to 8+3.
  const redMap=new Map(DATA.probabilities.red.map(x=>[x.number,x]));
  const blueMap=new Map(DATA.probabilities.blue.map(x=>[x.number,x]));
  const core=DATA.prediction.schemes.find(x=>x.code==='61');
  const ext=DATA.prediction.schemes.find(x=>x.code==='93')||DATA.prediction.schemes.find(x=>x.code==='83');
  if(ext){
    const reds=[2,9,10,13,18,22,25,28],blues=[11,3,15];
    ext.code='83';ext.label='8个红球 + 3个蓝球';ext.note='扩大红球与蓝球覆盖范围';
    ext.reds=reds;ext.blues=blues;ext.redCount=8;ext.blueCount=3;ext.bets=84;
    ext.additions=reds.filter(n=>!core.reds.includes(n));
    ext.redDetails=reds.map(n=>{const x=redMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.redCount,recent20:x.recent20,recent50:x.recent50,gap:x.gap,status:x.status,role:core.reds.includes(n)?'核心':'扩展'}});
    ext.blueDetails=blues.map(n=>{const x=blueMap.get(n);return {number:n,rank:x.rank,prob:x.modelProb,lift:x.lift,history:x.count,recent20:x.recent20,gap:x.gap}});
  }
  if(typeof window.renderSchemes==='function')window.renderSchemes(false);

  // Probability cards: number + next-draw probability only.
  window.renderProbability=function(){
    const red=document.getElementById('redHeat'),blue=document.getElementById('blueHeat');
    if(red){red.className='probability-board';red.innerHTML=DATA.probabilities.red.map(x=>'<div class="next-prob"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>').join('')}
    if(blue){blue.className='probability-board';blue.innerHTML=DATA.probabilities.blue.map(x=>'<div class="next-prob blue"><span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>').join('')}
  };
  window.renderProbability();

  // Fill the unused lower space with direct, non-technical result information.
  const main=document.querySelector('#forecast .forecast-main');
  if(main && !document.getElementById('forecastKeyInfo')){
    const s=DATA.prediction.schemes[0],last=DATA.meta.lastDraw;
    const sum=s.reds.reduce((a,b)=>a+b,0),odd=s.reds.filter(n=>n%2).length;
    const repeats=s.reds.filter(n=>last.reds.includes(n));
    const balls=last.reds.map(n=>'<span class="key-info-ball">'+pad(n)+'</span>').join('')+'<span style="color:#7890ad">+</span><span class="key-info-ball blue">'+pad(last.blue)+'</span>';
    const block=document.createElement('section');block.id='forecastKeyInfo';block.className='forecast-key-info';
    block.innerHTML='<h3>主推组概况</h3><div class="key-info-grid">'
      +'<div class="key-info-item"><span class="key-label">上期开奖号码</span><div class="key-info-balls">'+balls+'</div></div>'
      +'<div class="key-info-item"><span class="key-label">红球和值</span><span class="key-value">'+sum+'</span></div>'
      +'<div class="key-info-item"><span class="key-label">红球奇偶</span><span class="key-value">'+odd+' : '+(6-odd)+'</span></div>'
      +'<div class="key-info-item"><span class="key-label">与上期相同红球</span><span class="key-value">'+(repeats.length?repeats.map(pad).join('、'):'无')+'</span></div>'
      +'</div>';
    main.appendChild(block);
  }

  // Keep the glossary/function documentation consistent with 8+3.
  document.querySelectorAll('.feature-card,.term-card-v7').forEach(card=>{
    const h=card.querySelector('h3'),p=card.querySelector('p');if(!h)return;
    if(h.textContent.includes('93方案')){h.textContent=h.textContent.replace('93方案','83方案');if(p)p.textContent='选择8个红球和3个蓝球，自动拆成84注，购买金额168元。';}
    if(h.textContent.includes('/ 93')){h.textContent=h.textContent.replace('/ 93','/ 83');if(p)p.textContent=p.textContent.replace(/93表示9个红球和3个蓝球/g,'83表示8个红球和3个蓝球');}
    if(p){p.textContent=p.textContent.replace(/62、71、72、81、82和93/g,'62、71、72、81、82和83').replace(/9个红球和3个蓝球/g,'8个红球和3个蓝球').replace(/252注/g,'84注').replace(/504元/g,'168元');}
  });

  // Re-apply number click behavior after rebuilding the scheme cards.
  ['primaryScheme','schemeGrid'].forEach(id=>{
    const root=document.getElementById(id);if(!root)return;
    root.onclick=e=>{
      const b=e.target.closest('.ball');if(!b)return;
      const card=b.closest('[data-scheme]');if(!card)return;
      const s=DATA.prediction.schemes.find(x=>x.code===card.dataset.scheme);if(!s)return;
      const red=s.reds.map(n=>{const r=redMap.get(n);return '<div class="reason-row-v6"><span class="num-dot">'+pad(n)+'</span><div class="main"><b>'+(core.reds.includes(n)?'核心':'扩展')+'</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
      const blue=s.blues.map(n=>{const r=blueMap.get(n);return '<div class="reason-row-v6"><span class="num-dot blue">'+pad(n)+'</span><div class="main"><b>蓝球候选</b><div class="subline">近20期 '+r.recent20+' 次 · 已 '+r.gap+' 期没出 · 排名第 '+r.rank+'</div></div><div class="prob">'+pct(r.modelProb)+'</div></div>'}).join('');
      const heading=document.getElementById('reasonTitle'),body=document.getElementById('reasonBody'),modal=document.getElementById('reasonModal');
      if(heading)heading.textContent='预测原因';
      if(body)body.innerHTML='<div class="reason-summary"><div class="ball-row" style="margin:0">'+ballsHTML(s.reds,s.blues)+'</div><span class="cost">'+(s.bets*2)+'元 · '+s.bets+'注</span></div><div class="reason-columns"><div><h3>红球入选原因</h3><div class="reason-list">'+red+'</div></div><div><h3>蓝球入选原因</h3><div class="reason-list">'+blue+'</div></div></div>';
      if(modal)modal.classList.add('open');document.body.style.overflow='hidden';
    };
  });
})();



(function(){
  function renderSortedProbability(){
    const render=(id,items,isBlue)=>{
      const el=document.getElementById(id);if(!el)return;
      el.className='probability-board';
      const sorted=[...items].sort((a,b)=>(b.modelProb-a.modelProb)||(a.number-b.number));
      el.innerHTML=sorted.map((x,i)=>{
        const top=i<3?' prob-top-'+(i+1):'';
        const mark=i<3?'<span class="prob-rank-mark">'+(i+1)+'</span>':'';
        return '<div class="next-prob'+(isBlue?' blue':'')+top+'">'+mark+'<span class="prob-num">'+pad(x.number)+'</span><span class="prob-copy"><span class="prob-value">'+pct(x.modelProb)+'</span></span></div>';
      }).join('');
    };
    render('redHeat',DATA.probabilities.red,false);
    render('blueHeat',DATA.probabilities.blue,true);
  }
  window.renderProbability=renderSortedProbability;
  renderSortedProbability();
})();


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

  window.__drawVisibleCharts = drawVisibleCharts;
}
